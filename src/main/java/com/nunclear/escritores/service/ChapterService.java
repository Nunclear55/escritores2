package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Chapter;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.Volume;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ChapterRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.repository.VolumeRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private static final String PUBLICATION_STATE_DRAFT = "draft";
    private static final String PUBLICATION_STATE_PUBLISHED = "published";
    private static final String VISIBILITY_STATE_PUBLIC = "public";
    private static final String SORT_TITLE = "title";
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_UPDATED_AT = "updatedAt";
    private static final String SORT_POSITION_INDEX = "positionIndex";
    private static final String SORT_PUBLISHED_ON = "publishedOn";

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final VolumeRepository volumeRepository;
    private final AppUserRepository appUserRepository;

    public CreateChapterResponse createChapter(CreateChapterRequest request) {
        Story story = getEditableStory(request.storyId());

        validatePublicationState(request.publicationState());
        validateVolumeBelongsToStory(request.volumeId(), story.getId());

        Chapter chapter = new Chapter();
        chapter.setStoryId(request.storyId());
        chapter.setVolumeId(request.volumeId());
        chapter.setTitle(request.title());
        chapter.setSubtitle(request.subtitle());
        chapter.setContent(request.content());
        chapter.setPublishedOn(request.publishedOn());
        chapter.setPublicationState(request.publicationState().toLowerCase());
        chapter.setPositionIndex(request.positionIndex());

        int wordCount = calculateWordCount(request.content());
        chapter.setWordCount(wordCount);
        chapter.setReadingMinutes(calculateReadingMinutes(wordCount));

        Chapter saved = chapterRepository.save(chapter);

        return new CreateChapterResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getTitle(),
                saved.getPublicationState(),
                saved.getReadingMinutes(),
                saved.getWordCount()
        );
    }

    public ChapterDetailResponse getChapterById(Integer id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capítulo no encontrado"));

        validateReadAccess(chapter);

        return new ChapterDetailResponse(
                chapter.getId(),
                chapter.getStoryId(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getPublicationState(),
                chapter.getWordCount()
        );
    }

    public PageResponse<ChapterListItemResponse> getChaptersByStory(
            Integer storyId,
            boolean includeDrafts,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? SORT_POSITION_INDEX + ",asc" : sort
        );

        Page<Chapter> result;
        if (includeDrafts && canSeeDrafts(storyId)) {
            result = chapterRepository.findPageActiveByStoryId(storyId, pageable);
        } else {
            result = chapterRepository.findPagePublishedByStoryId(storyId, pageable);
        }

        return new PageResponse<>(
                result.getContent().stream()
                        .map(ch -> new ChapterListItemResponse(
                                ch.getId(),
                                ch.getTitle(),
                                ch.getPositionIndex(),
                                ch.getPublicationState(),
                                ch.getArchivedAt()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<ChapterListItemResponse> getPublishedChaptersByStory(
            Integer storyId,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? SORT_POSITION_INDEX + ",asc" : sort
        );

        Page<Chapter> result = chapterRepository.findPagePublishedByStoryId(storyId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(ch -> new ChapterListItemResponse(
                                ch.getId(),
                                ch.getTitle(),
                                ch.getPositionIndex(),
                                ch.getPublicationState(),
                                ch.getArchivedAt()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<ChapterListItemResponse> getMyDrafts(
            Integer storyId,
            int page,
            int size,
            String sort
    ) {
        AppUser currentUser = getAuthenticatedUser();
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? SORT_UPDATED_AT + ",desc" : sort);

        if (storyId == null) {
            List<Integer> ownStoryIds = storyRepository.findAll().stream()
                    .filter(s -> s.getOwnerUserId().equals(currentUser.getId()))
                    .map(Story::getId)
                    .toList();

            if (ownStoryIds.isEmpty()) {
                return new PageResponse<>(List.of(), page, size, 0, 0);
            }

            Page<Chapter> result = chapterRepository.findDraftsByStoryIds(ownStoryIds, pageable);
            return mapChapterPage(result);
        }

        Story story = getEditableStory(storyId);
        Page<Chapter> result = chapterRepository.findDraftsByStoryId(story.getId(), pageable);
        return mapChapterPage(result);
    }

    public PageResponse<ChapterSearchItemResponse> searchChapters(
            String q,
            Integer storyId,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? SORT_CREATED_AT + ",desc" : sort);

        Page<Chapter> result = chapterRepository.searchPublishedChapters(
                q == null ? "" : q,
                storyId,
                pageable
        );

        return new PageResponse<>(
                result.getContent().stream()
                        .map(ch -> new ChapterSearchItemResponse(
                                ch.getId(),
                                ch.getTitle(),
                                buildExcerpt(ch.getContent())
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateChapterResponse updateChapter(Integer id, UpdateChapterRequest request) {
        Chapter chapter = getEditableChapter(id);
        validateVolumeBelongsToStory(request.volumeId(), chapter.getStoryId());

        chapter.setTitle(request.title());
        chapter.setSubtitle(request.subtitle());
        chapter.setContent(request.content());
        chapter.setVolumeId(request.volumeId());
        chapter.setPositionIndex(request.positionIndex());

        int wordCount = calculateWordCount(request.content());
        chapter.setWordCount(wordCount);
        chapter.setReadingMinutes(calculateReadingMinutes(wordCount));

        Chapter saved = chapterRepository.save(chapter);

        return new UpdateChapterResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getUpdatedAt()
        );
    }

    public ChapterPublicationStateResponse publishChapter(Integer id) {
        Chapter chapter = getEditableChapter(id);
        chapter.setPublicationState(PUBLICATION_STATE_PUBLISHED);
        if (chapter.getPublishedOn() == null) {
            chapter.setPublishedOn(java.time.LocalDate.now());
        }
        Chapter saved = chapterRepository.save(chapter);

        return new ChapterPublicationStateResponse(saved.getId(), saved.getPublicationState());
    }

    public ChapterPublicationStateResponse unpublishChapter(Integer id) {
        Chapter chapter = getEditableChapter(id);
        chapter.setPublicationState(PUBLICATION_STATE_DRAFT);
        Chapter saved = chapterRepository.save(chapter);

        return new ChapterPublicationStateResponse(saved.getId(), saved.getPublicationState());
    }

    public ChapterArchiveResponse archiveChapter(Integer id) {
        Chapter chapter = getEditableChapter(id);
        chapter.setArchivedAt(LocalDateTime.now());
        Chapter saved = chapterRepository.save(chapter);

        return new ChapterArchiveResponse(saved.getId(), saved.getArchivedAt());
    }

    public MessageResponse reorderChapters(ReorderChaptersRequest request) {
        Story story = getEditableStory(request.storyId());

        Map<Integer, Integer> requestedPositions = request.items().stream()
                .collect(Collectors.toMap(ReorderChapterItemRequest::chapterId, ReorderChapterItemRequest::positionIndex));

        List<Chapter> chapters = chapterRepository.findAllById(requestedPositions.keySet());
        if (chapters.size() != request.items().size()) {
            throw new BadRequestException("Uno o más capítulos no existen");
        }

        for (Chapter chapter : chapters) {
            if (!chapter.getStoryId().equals(story.getId())) {
                throw new BadRequestException("Todos los capítulos deben pertenecer a la historia indicada");
            }
            if (chapter.getArchivedAt() != null) {
                throw new BadRequestException("No se puede reordenar capítulos archivados");
            }
            chapter.setPositionIndex(requestedPositions.get(chapter.getId()));
        }

        chapterRepository.saveAll(chapters);
        return new MessageResponse("Capítulos reordenados correctamente");
    }

    public MoveChapterResponse moveChapter(Integer id, MoveChapterRequest request) {
        Chapter chapter = getEditableChapter(id);

        Volume targetVolume = volumeRepository.findByIdAndStoryId(request.targetVolumeId(), chapter.getStoryId())
                .orElseThrow(() -> new BadRequestException("El volumen destino no existe o no pertenece a la historia"));

        chapter.setVolumeId(targetVolume.getId());
        chapter.setPositionIndex(request.newPositionIndex());

        Chapter saved = chapterRepository.save(chapter);

        return new MoveChapterResponse(
                saved.getId(),
                saved.getVolumeId(),
                saved.getPositionIndex()
        );
    }

    public MessageResponse deleteChapter(Integer id) {
        Chapter chapter = getEditableChapter(id);
        chapterRepository.delete(chapter);
        return new MessageResponse("Capítulo eliminado correctamente");
    }

    private PageResponse<ChapterListItemResponse> mapChapterPage(Page<Chapter> result) {
        return new PageResponse<>(
                result.getContent().stream()
                        .map(ch -> new ChapterListItemResponse(
                                ch.getId(),
                                ch.getTitle(),
                                ch.getPositionIndex(),
                                ch.getPublicationState(),
                                ch.getArchivedAt()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void validateReadAccess(Chapter chapter) {
        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Historia no encontrada"));

        boolean publicReadable =
                chapter.getArchivedAt() == null
                        && PUBLICATION_STATE_PUBLISHED.equalsIgnoreCase(chapter.getPublicationState())
                        && VISIBILITY_STATE_PUBLIC.equalsIgnoreCase(story.getVisibilityState())
                        && PUBLICATION_STATE_PUBLISHED.equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("Capítulo no encontrado");
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new ResourceNotFoundException("Capítulo no encontrado");
        }
    }

    private boolean canSeeDrafts(Integer storyId) {
        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            return false;
        }

        Story story = storyRepository.findById(storyId).orElse(null);
        if (story == null) {
            return false;
        }

        return story.getOwnerUserId().equals(currentUser.getId()) || isModeratorOrAdmin(currentUser);
    }

    private Story getEditableStory(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Historia no encontrada"));

        AppUser currentUser = getAuthenticatedUser();
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre esta historia");
        }

        return story;
    }

    private Chapter getEditableChapter(Integer chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Capítulo no encontrado"));

        Story story = getEditableStory(chapter.getStoryId());

        if (!story.getId().equals(chapter.getStoryId())) {
            throw new UnauthorizedException("No tienes permisos sobre este capítulo");
        }

        return chapter;
    }

    private void validateVolumeBelongsToStory(Integer volumeId, Integer storyId) {
        if (volumeId == null) {
            return;
        }

        volumeRepository.findByIdAndStoryId(volumeId, storyId)
                .orElseThrow(() -> new BadRequestException("El volumen no pertenece a la historia"));
    }

    private void validatePublicationState(String publicationState) {
        if (!PUBLICATION_STATE_DRAFT.equalsIgnoreCase(publicationState)
                && !PUBLICATION_STATE_PUBLISHED.equalsIgnoreCase(publicationState)) {
            throw new BadRequestException("publicationState inválido");
        }
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
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

    private int calculateWordCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }

    private int calculateReadingMinutes(int wordCount) {
        if (wordCount <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(wordCount / 200.0));
    }

    private String buildExcerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "...";
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
            case SORT_TITLE -> SORT_TITLE;
            case SORT_CREATED_AT -> SORT_CREATED_AT;
            case SORT_UPDATED_AT -> SORT_UPDATED_AT;
            case SORT_POSITION_INDEX -> SORT_POSITION_INDEX;
            case SORT_PUBLISHED_ON -> SORT_PUBLISHED_ON;
            default -> SORT_POSITION_INDEX;
        };
    }
}