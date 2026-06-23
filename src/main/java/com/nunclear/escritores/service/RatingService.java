package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class RatingService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String RATING_NOT_FOUND = "Calificación no encontrada";

    private final StoryRatingRepository storyRatingRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public RatingResponse upsertRating(UpsertRatingRequest request) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "updatedAt", "scoreValue");
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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Double avg = storyRatingRepository.findAverageScoreByStoryId(storyId);
        long count = storyRatingRepository.countByStoryId(storyId);

        double rounded = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;

        return new RatingAverageResponse(rounded, count);
    }

    public RatingResponse getMyRating(Integer storyId) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

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

        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        boolean isOwner = rating.getAuthorUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = AuthUtils.isModeratorOrAdmin(currentUser);

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

        boolean publicReadable = StoryAccessUtils.isPublicReadable(story);

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = AuthUtils.isModeratorOrAdmin(currentUser);

        if (!publicReadable && !isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No puedes calificar esta historia");
        }
    }

}