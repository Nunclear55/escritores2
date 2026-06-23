package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.dto.request.ReplaceMediaRequest;
import com.nunclear.escritores.dto.request.UploadMediaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class MediaService {

    // Mala práctica corregida:
    // literales duplicados ("magic strings").
    // Tipo: duplicación de cadenas / baja mantenibilidad.
    private static final String FILE_NOT_FOUND = "Archivo no encontrado";
    private static final String CHAPTER_NOT_FOUND = "Capítulo no encontrado";

    private final MediaRepository mediaRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public UploadMediaResponse uploadMedia(UploadMediaRequest request) {
        Chapter chapter = getEditableChapter(request.chapterId());

        Media media = new Media();
        media.setFilename(generateStoredFilename(request.originalFilename()));
        media.setOriginalFilename(request.originalFilename());
        media.setMediaKind(request.mediaKind().toLowerCase(Locale.ROOT));
        media.setDescription(request.description());
        media.setChapterId(chapter.getId());
        media.setStoragePath(request.storagePath());

        Media saved = mediaRepository.save(media);

        return new UploadMediaResponse(
                saved.getId(),
                saved.getFilename(),
                saved.getOriginalFilename(),
                saved.getMediaKind(),
                saved.getChapterId()
        );
    }

    public MediaDetailResponse getMediaById(Integer id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FILE_NOT_FOUND));

        Chapter chapter = chapterRepository.findById(media.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_NOT_FOUND));

        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(chapter, story, appUserRepository);

        return new MediaDetailResponse(
                media.getId(),
                media.getFilename(),
                media.getOriginalFilename(),
                media.getMediaKind(),
                media.getDescription(),
                media.getChapterId(),
                media.getStoragePath()
        );
    }

    public PageResponse<MediaListItemResponse> getMediaByChapter(
            Integer chapterId,
            int page,
            int size,
            String sort
    ) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_NOT_FOUND));

        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(chapter, story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "updatedAt", "filename", "mediaKind");
        Page<Media> result = mediaRepository.findByChapterId(chapterId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(media -> new MediaListItemResponse(
                                media.getId(),
                                media.getFilename(),
                                media.getMediaKind()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public ReplaceMediaResponse replaceMedia(Integer id, ReplaceMediaRequest request) {
        Media media = getEditableMedia(id);

        media.setFilename(generateStoredFilename(request.originalFilename()));
        media.setOriginalFilename(request.originalFilename());
        media.setDescription(request.description());
        media.setStoragePath(request.storagePath());

        Media saved = mediaRepository.save(media);

        return new ReplaceMediaResponse(
                saved.getId(),
                saved.getFilename(),
                saved.getDescription()
        );
    }

    public MediaDownloadResponse downloadMedia(Integer id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FILE_NOT_FOUND));

        Chapter chapter = chapterRepository.findById(media.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_NOT_FOUND));

        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(chapter, story, appUserRepository);

        return new MediaDownloadResponse(media.getStoragePath());
    }

    public MessageResponse deleteMedia(Integer id) {
        Media media = getEditableMedia(id);
        mediaRepository.delete(media);
        return new MessageResponse("Archivo eliminado correctamente");
    }

    private Media getEditableMedia(Integer id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FILE_NOT_FOUND));

        getEditableChapter(media.getChapterId());
        return media;
    }

    private Chapter getEditableChapter(Integer chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_NOT_FOUND));

        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = AuthUtils.isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre este capítulo");
        }

        return chapter;
    }

    private void validateReadAccess(Chapter chapter, Story story) {
        boolean publicReadable =
                chapter.getArchivedAt() == null
                        && "published".equalsIgnoreCase(chapter.getPublicationState())
                        && StoryAccessUtils.isPublicReadable(story);

        if (!publicReadable && !StoryAccessUtils.canReadStory(story, appUserRepository)) {
            throw new ResourceNotFoundException(FILE_NOT_FOUND);
        }
    }

    private String generateStoredFilename(String originalFilename) {
        String clean = sanitizeFilename(originalFilename);
        int dotIndex = clean.lastIndexOf('.');
        String base = dotIndex > 0 ? clean.substring(0, dotIndex) : clean;
        String ext = dotIndex > 0 ? clean.substring(dotIndex) : "";
        return base + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
    }

    private String sanitizeFilename(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
        return withoutAccents
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
    }
}