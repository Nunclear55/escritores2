package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateStoryRequest;
import com.nunclear.escritores.dto.request.DuplicateStoryRequest;
import com.nunclear.escritores.dto.request.UpdateStoryRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StoryService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String STORY_NOT_FOUND = "Historia no encontrada";
    private static final String PUBLICATION_PUBLISHED = "published";
    private static final String PUBLICATION_DRAFT = "draft";
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String ACCESS_MODERATOR = "moderator";
    private static final String ACCESS_ADMIN = "admin";
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String DEFAULT_CREATED_DESC_SORT = "createdAt,desc";

    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateStoryResponse createStory(CreateStoryRequest request) {
        AppUser currentUser = getAuthenticatedUser();

        validateVisibilityState(request.visibilityState());
        validatePublicationState(request.publicationState());

        Story story = new Story();
        story.setOwnerUserId(currentUser.getId());
        story.setTitle(request.title());
        story.setSlugText(generateUniqueSlug(request.title()));
        story.setDescription(request.description());
        story.setCoverImageUrl(request.coverImageUrl());
        story.setVisibilityState(request.visibilityState().toLowerCase(Locale.ROOT));
        story.setPublicationState(request.publicationState().toLowerCase(Locale.ROOT));
        story.setAllowFeedback(request.allowFeedback());
        story.setAllowScores(request.allowScores());
        story.setStartedOn(request.startedOn());

        if (PUBLICATION_PUBLISHED.equalsIgnoreCase(request.publicationState())) {
            story.setPublishedAt(LocalDateTime.now());
        }

        Story saved = storyRepository.save(story);

        return new CreateStoryResponse(
                saved.getId(),
                saved.getOwnerUserId(),
                saved.getTitle(),
                saved.getSlugText(),
                saved.getVisibilityState(),
                saved.getPublicationState(),
                saved.getCreatedAt()
        );
    }

    public StoryDetailResponse getStoryById(Integer id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new StoryDetailResponse(
                story.getId(),
                story.getOwnerUserId(),
                story.getTitle(),
                story.getSlugText(),
                story.getDescription(),
                story.getVisibilityState(),
                story.getPublicationState()
        );
    }

    public StorySlugResponse getStoryBySlug(String slug) {
        Story story = storyRepository.findBySlugText(slug)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new StorySlugResponse(
                story.getId(),
                story.getSlugText(),
                story.getTitle()
        );
    }

    public PageResponse<StoryListItemResponse> listPublicStories(int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort);

        Page<Story> result = storyRepository.findByVisibilityStateIgnoreCaseAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                VISIBILITY_PUBLIC,
                PUBLICATION_PUBLISHED,
                pageable
        );

        return new PageResponse<>(
                result.getContent().stream()
                        .map(story -> new StoryListItemResponse(
                                story.getId(),
                                story.getTitle(),
                                story.getSlugText(),
                                story.getVisibilityState(),
                                story.getPublicationState()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<StoryListItemResponse> searchStories(
            String q,
            String visibilityState,
            String publicationState,
            int page,
            int size,
            String sort
    ) {
        String finalVisibility = visibilityState == null || visibilityState.isBlank() ? VISIBILITY_PUBLIC : visibilityState;
        String finalPublication = publicationState == null || publicationState.isBlank() ? PUBLICATION_PUBLISHED : publicationState;

        if (!VISIBILITY_PUBLIC.equalsIgnoreCase(finalVisibility) || !PUBLICATION_PUBLISHED.equalsIgnoreCase(finalPublication)) {
            throw new BadRequestException("Solo se permite búsqueda pública de historias publicadas");
        }

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort);

        Page<Story> result = storyRepository.searchPublicStories(
                q == null ? "" : q,
                finalVisibility,
                finalPublication,
                pageable
        );

        return new PageResponse<>(
                result.getContent().stream()
                        .map(story -> new StoryListItemResponse(
                                story.getId(),
                                story.getTitle(),
                                story.getSlugText(),
                                story.getVisibilityState(),
                                story.getPublicationState()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<UserStorySummaryResponse> getStoriesByUser(
            Integer userId,
            boolean includeDrafts,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort);

        Page<Story> result;
        if (canSeePrivateStories(userId) && includeDrafts) {
            result = storyRepository.findAllVisibleForOwner(userId, pageable);
        } else {
            result = storyRepository.findPublicPublishedByOwner(userId, pageable);
        }

        return new PageResponse<>(
                result.getContent().stream()
                        .map(story -> new UserStorySummaryResponse(
                                story.getId(),
                                story.getOwnerUserId(),
                                story.getTitle(),
                                story.getPublicationState()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<UserStorySummaryResponse> getMyDrafts(int page, int size, String sort) {
        AppUser currentUser = getAuthenticatedUser();
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "updatedAt,desc" : sort);

        Page<Story> result = storyRepository.findByOwnerUserIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                currentUser.getId(),
                PUBLICATION_DRAFT,
                pageable
        );

        return new PageResponse<>(
                result.getContent().stream()
                        .map(story -> new UserStorySummaryResponse(
                                story.getId(),
                                story.getOwnerUserId(),
                                story.getTitle(),
                                story.getPublicationState()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<ArchivedStoryItemResponse> getMyArchived(int page, int size, String sort) {
        AppUser currentUser = getAuthenticatedUser();
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "updatedAt,desc" : sort);

        Page<Story> result = storyRepository.findByOwnerUserIdAndArchivedAtIsNotNull(currentUser.getId(), pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(story -> new ArchivedStoryItemResponse(
                                story.getId(),
                                story.getTitle(),
                                story.getArchivedAt()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateStoryResponse updateStory(Integer id, UpdateStoryRequest request) {
        Story story = getEditableStory(id);

        validateVisibilityState(request.visibilityState());

        story.setTitle(request.title());
        story.setSlugText(generateUniqueSlugForExisting(request.title(), story.getId()));
        story.setDescription(request.description());
        story.setCoverImageUrl(request.coverImageUrl());
        story.setVisibilityState(request.visibilityState().toLowerCase(Locale.ROOT));
        story.setAllowFeedback(request.allowFeedback());
        story.setAllowScores(request.allowScores());

        Story saved = storyRepository.save(story);

        return new UpdateStoryResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getUpdatedAt()
        );
    }

    public StoryPublicationResponse publishStory(Integer id) {
        Story story = getEditableStory(id);

        story.setPublicationState(PUBLICATION_PUBLISHED);
        story.setPublishedAt(LocalDateTime.now());

        Story saved = storyRepository.save(story);

        return new StoryPublicationResponse(
                saved.getId(),
                saved.getPublicationState(),
                saved.getPublishedAt()
        );
    }

    public StoryPublicationResponse unpublishStory(Integer id) {
        Story story = getEditableStory(id);

        story.setPublicationState(PUBLICATION_DRAFT);
        story.setPublishedAt(null);

        Story saved = storyRepository.save(story);

        return new StoryPublicationResponse(
                saved.getId(),
                saved.getPublicationState(),
                saved.getPublishedAt()
        );
    }

    public StoryArchiveResponse archiveStory(Integer id) {
        Story story = getEditableStory(id);

        story.setArchivedAt(LocalDateTime.now());

        Story saved = storyRepository.save(story);

        return new StoryArchiveResponse(
                saved.getId(),
                saved.getArchivedAt()
        );
    }

    public StoryArchiveResponse restoreStory(Integer id) {
        Story story = getEditableStory(id);

        story.setArchivedAt(null);

        Story saved = storyRepository.save(story);

        return new StoryArchiveResponse(
                saved.getId(),
                saved.getArchivedAt()
        );
    }

    public DuplicateStoryResponse duplicateStory(Integer id, DuplicateStoryRequest request) {
        Story source = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(source);

        AppUser currentUser = getAuthenticatedUser();

        Story duplicate = new Story();
        duplicate.setOwnerUserId(currentUser.getId());
        duplicate.setTitle(request.title());
        duplicate.setSlugText(generateUniqueSlug(request.title()));
        duplicate.setDescription(source.getDescription());
        duplicate.setCoverImageUrl(source.getCoverImageUrl());
        duplicate.setVisibilityState(source.getVisibilityState());
        duplicate.setPublicationState(PUBLICATION_DRAFT);
        duplicate.setAllowFeedback(source.getAllowFeedback());
        duplicate.setAllowScores(source.getAllowScores());
        duplicate.setStartedOn(source.getStartedOn());
        duplicate.setPublishedAt(null);
        duplicate.setArchivedAt(null);

        Story saved = storyRepository.save(duplicate);

        return new DuplicateStoryResponse(
                saved.getId(),
                source.getId(),
                saved.getTitle(),
                saved.getPublicationState()
        );
    }

    public MessageResponse deleteStory(Integer id) {
        Story story = getEditableStory(id);
        storyRepository.delete(story);
        return new MessageResponse("Historia eliminada correctamente");
    }

    private Story getEditableStory(Integer id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        AppUser currentUser = getAuthenticatedUser();
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos para modificar esta historia");
        }

        return story;
    }

    private void validateReadAccess(Story story) {
        boolean publicPublished =
                VISIBILITY_PUBLIC.equalsIgnoreCase(story.getVisibilityState())
                        && PUBLICATION_PUBLISHED.equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicPublished) {
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

    private boolean canSeePrivateStories(Integer userId) {
        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            return false;
        }

        return currentUser.getId().equals(userId) || isModeratorOrAdmin(currentUser);
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        // Mala práctica corregida:
        // strings mágicos repetidos para roles.
        // Tipo: duplicación de literales / lógica repetida.
        return ACCESS_MODERATOR.equals(user.getAccessLevel().name())
                || ACCESS_ADMIN.equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / validación defensiva insuficiente.
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
        // catch vacío o demasiado genérico para ocultar errores.
        // Tipo: swallowing exceptions / ocultamiento silencioso de fallos.
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

    private void validateVisibilityState(String visibilityState) {
        if (!VISIBILITY_PUBLIC.equalsIgnoreCase(visibilityState) && !"private".equalsIgnoreCase(visibilityState)) {
            throw new BadRequestException("visibilityState inválido");
        }
    }

    private void validatePublicationState(String publicationState) {
        if (!PUBLICATION_DRAFT.equalsIgnoreCase(publicationState)
                && !PUBLICATION_PUBLISHED.equalsIgnoreCase(publicationState)) {
            throw new BadRequestException("publicationState inválido");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case SORT_CREATED_AT -> SORT_CREATED_AT;
            case "updatedAt" -> "updatedAt";
            case "title" -> "title";
            case "publishedAt" -> "publishedAt";
            default -> SORT_CREATED_AT;
        };
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = slugify(title);
        String finalSlug = baseSlug;
        int counter = 2;

        while (storyRepository.existsBySlugText(finalSlug)) {
            finalSlug = baseSlug + "-" + counter;
            counter++;
        }

        return finalSlug;
    }

    private String generateUniqueSlugForExisting(String title, Integer currentStoryId) {
        String baseSlug = slugify(title);
        String candidate = baseSlug;
        int counter = 2;

        while (true) {
            Story existing = storyRepository.findBySlugText(candidate).orElse(null);
            if (existing == null || existing.getId().equals(currentStoryId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + counter;
            counter++;
        }
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");

        // Mala práctica corregida:
        // precedencia implícita en regex.
        // Tipo: expresión regular poco clara / legibilidad y riesgo de errores.
        String slug = withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");

        if (slug.isBlank()) {
            throw new BadRequestException("No se pudo generar slug para el título");
        }

        return slug;
    }
}