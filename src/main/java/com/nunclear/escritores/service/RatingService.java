package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.UpsertRatingRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.StoryRating;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryRatingRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String STORY_NOT_FOUND = "Historia no encontrada";
    private static final String RATING_NOT_FOUND = "Calificación no encontrada";

    private final StoryRatingRepository storyRatingRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public RatingResponse upsertRating(UpsertRatingRequest request) {
        AppUser currentUser = getAuthenticatedUser();

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateCanRate(story, currentUser);

        StoryRating rating = storyRatingRepository
                .findByStoryIdAndAuthorUserId(story.getId(), currentUser.getId())
                .orElseGet(StoryRating::new);

        rating.setStoryId(story.getId());
        rating.setAuthorUserId(currentUser.getId());
        rating.setScoreValue(request.scoreValue());
        rating.setReviewText(request.reviewText());

        StoryRating saved = storyRatingRepository.save(rating);

        return new RatingResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getAuthorUserId(),
                saved.getScoreValue(),
                saved.getReviewText()
        );
    }

    public RatingResponse getRatingById(Integer id) {
        StoryRating rating = storyRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RATING_NOT_FOUND));

        Story story = storyRepository.findById(rating.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new RatingResponse(
                rating.getId(),
                rating.getStoryId(),
                rating.getAuthorUserId(),
                rating.getScoreValue(),
                rating.getReviewText()
        );
    }

    public PageResponse<RatingListItemResponse> getRatingsByStory(Integer storyId, int page, int size, String sort) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<StoryRating> result = storyRatingRepository.findByStoryId(storyId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(rating -> new RatingListItemResponse(
                                rating.getId(),
                                rating.getAuthorUserId(),
                                rating.getScoreValue(),
                                rating.getReviewText()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public RatingAverageResponse getAverageByStory(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Double avg = storyRatingRepository.findAverageScoreByStoryId(storyId);
        long count = storyRatingRepository.countByStoryId(storyId);

        double rounded = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;

        return new RatingAverageResponse(rounded, count);
    }

    public RatingResponse getMyRating(Integer storyId) {
        AppUser currentUser = getAuthenticatedUser();

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        StoryRating rating = storyRatingRepository.findByStoryIdAndAuthorUserId(storyId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(RATING_NOT_FOUND));

        return new RatingResponse(
                rating.getId(),
                rating.getStoryId(),
                rating.getAuthorUserId(),
                rating.getScoreValue(),
                rating.getReviewText()
        );
    }

    public MessageResponse deleteRating(Integer id) {
        StoryRating rating = storyRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RATING_NOT_FOUND));

        AppUser currentUser = getAuthenticatedUser();

        boolean isOwner = rating.getAuthorUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre esta calificación");
        }

        storyRatingRepository.delete(rating);
        return new MessageResponse("Calificación eliminada correctamente");
    }

    private void validateCanRate(Story story, AppUser currentUser) {
        if (!Boolean.TRUE.equals(story.getAllowScores())) {
            throw new BadRequestException("La historia no permite calificaciones");
        }

        boolean publicReadable =
                "public".equalsIgnoreCase(story.getVisibilityState())
                        && "published".equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!publicReadable && !isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No puedes calificar esta historia");
        }
    }

    private void validateReadAccess(Story story) {
        boolean publicReadable =
                "public".equalsIgnoreCase(story.getVisibilityState())
                        && "published".equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / falta de validación defensiva.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("No autenticado");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No autenticado");
        }

        return appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private AppUser tryGetAuthenticatedUser() {
        // Mala práctica corregida:
        // catch vacío.
        // Tipo: swallowing exceptions / ocultamiento silencioso de errores.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return appUserRepository.findById(userDetails.getId()).orElse(null);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "updatedAt" -> "updatedAt";
            case "scoreValue" -> "scoreValue";
            default -> "createdAt";
        };
    }
}