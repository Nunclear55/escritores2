package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.ReplaceMediaRequest;
import com.nunclear.escritores.dto.request.UploadMediaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MediaService {

    // Mala práctica corregida:
    // literales duplicados ("magic strings").
    // Tipo: duplicación de cadenas / baja mantenibilidad.
    private static final String FILE_NOT_FOUND = "Archivo no encontrado";
    private static final String CHAPTER_NOT_FOUND = "Capítulo no encontrado";
    private static final String STORY_NOT_FOUND = "Historia no encontrada";

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
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(chapter, story);

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
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(chapter, story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
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
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(chapter, story);

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
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        AppUser currentUser = getAuthenticatedUser();
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre este capítulo");
        }

        return chapter;
    }

    private void validateReadAccess(Chapter chapter, Story story) {
        boolean publicReadable =
                chapter.getArchivedAt() == null
                        && "published".equalsIgnoreCase(chapter.getPublicationState())
                        && "public".equalsIgnoreCase(story.getVisibilityState())
                        && "published".equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(FILE_NOT_FOUND);
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new ResourceNotFoundException(FILE_NOT_FOUND);
        }
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / falta de programación defensiva.
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
        // bloque catch vacío.
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
            case "filename" -> "filename";
            case "mediaKind" -> "mediaKind";
            default -> "createdAt";
        };
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