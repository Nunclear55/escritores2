package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.dto.request.RegisterChapterViewRequest;
import com.nunclear.escritores.dto.request.RegisterStoryViewRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import com.nunclear.escritores.util.StoryAccessUtils;

@Service
@RequiredArgsConstructor
public class MetricsService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String CHAPTER_NOT_FOUND = "Capítulo no encontrado";
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String PUBLICATION_PUBLISHED = "published";

    private final StoryViewLogRepository storyViewLogRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryFavoriteRepository storyFavoriteRepository;
    private final StoryRatingRepository storyRatingRepository;
    private final StoryCommentRepository storyCommentRepository;
    private final AppUserRepository appUserRepository;

    public MessageResponse registerStoryView(RegisterStoryViewRequest request) {
        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        if (request.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new BadRequestException(CHAPTER_NOT_FOUND));

            if (!chapter.getStoryId().equals(story.getId())) {
                throw new BadRequestException("El capítulo no pertenece a la historia");
            }
        }

        StoryViewLog log = new StoryViewLog();
        log.setStoryId(story.getId());
        log.setChapterId(request.chapterId());
        log.setUserId(getAuthenticatedUserIdOrNull());
        log.setVisitorToken(request.visitorToken());
        log.setIpAddress(request.ipAddress());
        log.setUserAgentText(request.userAgentText());

        storyViewLogRepository.save(log);

        return new MessageResponse("Visita registrada");
    }

    public MessageResponse registerChapterView(RegisterChapterViewRequest request) {
        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_NOT_FOUND));

        if (!chapter.getStoryId().equals(story.getId())) {
            throw new BadRequestException("El capítulo no pertenece a la historia");
        }

        validateReadableChapter(chapter, story);

        StoryViewLog log = new StoryViewLog();
        log.setStoryId(story.getId());
        log.setChapterId(chapter.getId());
        log.setUserId(getAuthenticatedUserIdOrNull());
        log.setVisitorToken(request.visitorToken());
        log.setIpAddress(request.ipAddress());
        log.setUserAgentText(request.userAgentText());

        storyViewLogRepository.save(log);

        return new MessageResponse("Visita registrada");
    }

    public StoryMetricsResponse getStoryMetrics(Integer storyId) {
        Story story = StoryAccessUtils.getEditableStory(storyId, storyRepository, appUserRepository);

        long views = storyViewLogRepository.countByStoryId(storyId);
        long favorites = storyFavoriteRepository.countByStoryId(storyId);
        long ratingsCount = storyRatingRepository.countByStoryId(storyId);
        Double average = storyRatingRepository.findAverageScoreByStoryId(storyId);
        double roundedAverage = average == null ? 0.0 : Math.round(average * 10.0) / 10.0;

        return new StoryMetricsResponse(
                story.getId(),
                views,
                favorites,
                ratingsCount,
                roundedAverage
        );
    }

    public ChapterMetricsResponse getChapterMetrics(Integer chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_NOT_FOUND));

        StoryAccessUtils.getEditableStory(chapter.getStoryId(), storyRepository, appUserRepository);

        long views = storyViewLogRepository.countByChapterId(chapterId);
        long commentsCount = storyCommentRepository.countByChapterIdAndDeletedAtIsNull(chapterId);

        return new ChapterMetricsResponse(
                chapter.getId(),
                views,
                commentsCount
        );
    }

    private Optional<TopViewedStoryItemResponse> buildTopViewedStoryItem(Object[] row) {
        Integer storyId = (Integer) row[0];
        Long views = (Long) row[1];

        return storyRepository.findById(storyId)
                .filter(this::isPublicReadableStory)
                .map(story -> new TopViewedStoryItemResponse(
                        story.getId(),
                        story.getTitle(),
                        views
                ));
    }

    private boolean isPublicReadableStory(Story story) {
        return StoryAccessUtils.isPublicReadable(story);
    }

    public AuthorMetricsResponse getAuthorMetrics(Integer userId) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        boolean isOwner = currentUser.getId().equals(userId);
        boolean isModeratorOrAdmin = AuthUtils.isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos para ver estas métricas");
        }

        AppUser author = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        long totalViews = storyViewLogRepository.countViewsByAuthor(author.getId());

        return new AuthorMetricsResponse(author.getId(), totalViews);
    }

    public PageResponse<TopViewedStoryItemResponse> getTopViewedStories(int page, int size) {
        Page<Object[]> result = storyViewLogRepository.findTopViewedStories(PageRequest.of(page, size));

        var content = result.getContent().stream()
                .flatMap(row -> buildTopViewedStoryItem(row).stream())
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void validateReadableChapter(Chapter chapter, Story story) {
        boolean publicReadable =
                chapter.getArchivedAt() == null
                        && PUBLICATION_PUBLISHED.equalsIgnoreCase(chapter.getPublicationState())
                        && StoryAccessUtils.isPublicReadable(story);

        if (!publicReadable && !StoryAccessUtils.canReadStory(story, appUserRepository)) {
            throw new ResourceNotFoundException(CHAPTER_NOT_FOUND);
        }
    }

    private Integer getAuthenticatedUserIdOrNull() {
        AppUser user = AuthUtils.tryGetAuthenticatedUser(appUserRepository);
        return user != null ? user.getId() : null;
    }

}