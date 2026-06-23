package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateCommentRequest;
import com.nunclear.escritores.dto.request.UpdateCommentRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String MSG_STORY_NOT_FOUND = "Historia no encontrada";
    private static final String MSG_COMMENT_NOT_FOUND = "Comentario no encontrado";
    private static final String MSG_CHAPTER_NOT_FOUND = "Capítulo no encontrado";
    private static final String MSG_NOT_AUTHENTICATED = "No autenticado";

    private static final String VISIBILITY_PUBLIC = "public";
    private static final String VISIBILITY_VISIBLE = "visible";
    private static final String VISIBILITY_DELETED = "deleted";
    private static final String PUBLICATION_PUBLISHED = "published";

    private static final String ROLE_MODERATOR = "MODERATOR";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String SORT_UPDATED_AT = "updatedAt";
    private static final String SORT_EDITED_AT = "editedAt";
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_DESC = "desc";

    private final StoryCommentRepository storyCommentRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final AppUserRepository appUserRepository;

    public CreateCommentResponse createComment(CreateCommentRequest request) {
        AppUser currentUser = getAuthenticatedUser();

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException(MSG_STORY_NOT_FOUND));

        validateCanComment(story);

        if (request.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new BadRequestException(MSG_CHAPTER_NOT_FOUND));

            if (!chapter.getStoryId().equals(story.getId())) {
                throw new BadRequestException("El capítulo no pertenece a la historia");
            }
        }

        if (request.parentCommentId() != null) {
            StoryComment parent = storyCommentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new BadRequestException("Comentario padre no encontrado"));

            if (parent.getDeletedAt() != null) {
                throw new BadRequestException("No se puede responder a un comentario eliminado");
            }

            if (!parent.getStoryId().equals(story.getId())) {
                throw new BadRequestException("El comentario padre no pertenece a la historia");
            }
        }

        StoryComment comment = new StoryComment();
        comment.setStoryId(request.storyId());
        comment.setChapterId(request.chapterId());
        comment.setParentCommentId(request.parentCommentId());
        comment.setAuthorUserId(currentUser.getId());
        comment.setContent(request.content());
        comment.setVisibilityState(VISIBILITY_VISIBLE);

        StoryComment saved = storyCommentRepository.save(comment);

        return new CreateCommentResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getAuthorUserId(),
                saved.getContent(),
                saved.getVisibilityState()
        );
    }

    public CommentDetailResponse getCommentById(Integer id) {
        StoryComment comment = storyCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_COMMENT_NOT_FOUND));

        validateCommentReadable(comment);

        return new CommentDetailResponse(
                comment.getId(),
                comment.getStoryId(),
                comment.getChapterId(),
                comment.getAuthorUserId(),
                comment.getContent(),
                comment.getVisibilityState()
        );
    }

    public PageResponse<CommentListItemResponse> getCommentsByStory(Integer storyId, int page, int size, String sort) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_STORY_NOT_FOUND));

        validateStoryReadable(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<StoryComment> result =
                storyCommentRepository.findByStoryIdAndParentCommentIdIsNullAndDeletedAtIsNull(storyId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .filter(this::isVisibleComment)
                        .map(comment -> new CommentListItemResponse(
                                comment.getId(),
                                comment.getContent(),
                                comment.getAuthorUserId()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<CommentListItemResponse> getCommentsByChapter(Integer chapterId, int page, int size, String sort) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CHAPTER_NOT_FOUND));

        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(MSG_STORY_NOT_FOUND));

        validateChapterReadable(chapter, story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<StoryComment> result = storyCommentRepository.findByChapterIdAndDeletedAtIsNull(chapterId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .filter(this::isVisibleComment)
                        .map(comment -> new CommentListItemResponse(
                                comment.getId(),
                                comment.getContent(),
                                comment.getAuthorUserId()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<CommentReplyItemResponse> getReplies(Integer id, int page, int size, String sort) {
        StoryComment parent = storyCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_COMMENT_NOT_FOUND));

        validateCommentReadable(parent);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,asc" : sort);
        Page<StoryComment> result = storyCommentRepository.findByParentCommentIdAndDeletedAtIsNull(id, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .filter(this::isVisibleComment)
                        .map(reply -> new CommentReplyItemResponse(
                                reply.getId(),
                                reply.getParentCommentId(),
                                reply.getContent()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateCommentResponse updateComment(Integer id, UpdateCommentRequest request) {
        StoryComment comment = storyCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_COMMENT_NOT_FOUND));

        validateCanEditComment(comment);

        comment.setContent(request.content());
        comment.setEditedAt(LocalDateTime.now());

        StoryComment saved = storyCommentRepository.save(comment);

        return new UpdateCommentResponse(
                saved.getId(),
                saved.getContent(),
                saved.getEditedAt()
        );
    }

    public MessageResponse deleteComment(Integer id) {
        StoryComment comment = storyCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_COMMENT_NOT_FOUND));

        validateCanEditComment(comment);

        comment.setDeletedAt(LocalDateTime.now());
        comment.setVisibilityState(VISIBILITY_DELETED);
        storyCommentRepository.save(comment);

        return new MessageResponse("Comentario eliminado correctamente");
    }

    private void validateCanComment(Story story) {
        if (!Boolean.TRUE.equals(story.getAllowFeedback())) {
            throw new BadRequestException("La historia no permite comentarios");
        }

        boolean publicReadable = isPublicReadableStory(story);
        AppUser currentUser = tryGetAuthenticatedUser();

        if (!publicReadable && currentUser == null) {
            throw new UnauthorizedException(MSG_NOT_AUTHENTICATED);
        }

        if (!publicReadable && currentUser != null) {
            boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
            boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

            if (!isOwner && !isModeratorOrAdmin) {
                throw new UnauthorizedException("No tienes permisos para comentar en esta historia");
            }
        }
    }

    private void validateCommentReadable(StoryComment comment) {
        if (!isVisibleComment(comment)) {
            throw new ResourceNotFoundException(MSG_COMMENT_NOT_FOUND);
        }

        Story story = storyRepository.findById(comment.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(MSG_STORY_NOT_FOUND));

        if (comment.getChapterId() != null) {
            Chapter chapter = chapterRepository.findById(comment.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException(MSG_CHAPTER_NOT_FOUND));
            validateChapterReadable(chapter, story);
        } else {
            validateStoryReadable(story);
        }
    }

    private void validateStoryReadable(Story story) {
        if (isPublicReadableStory(story)) {
            return;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(MSG_STORY_NOT_FOUND);
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new ResourceNotFoundException(MSG_STORY_NOT_FOUND);
        }
    }

    private void validateChapterReadable(Chapter chapter, Story story) {
        boolean publicReadable =
                chapter.getArchivedAt() == null
                        && PUBLICATION_PUBLISHED.equalsIgnoreCase(chapter.getPublicationState())
                        && isPublicReadableStory(story);

        if (publicReadable) {
            return;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(MSG_CHAPTER_NOT_FOUND);
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new ResourceNotFoundException(MSG_CHAPTER_NOT_FOUND);
        }
    }

    private void validateCanEditComment(StoryComment comment) {
        AppUser currentUser = getAuthenticatedUser();

        boolean isOwner = comment.getAuthorUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre este comentario");
        }

        if (comment.getDeletedAt() != null) {
            throw new BadRequestException("El comentario ya fue eliminado");
        }
    }

    private boolean isPublicReadableStory(Story story) {
        return VISIBILITY_PUBLIC.equalsIgnoreCase(story.getVisibilityState())
                && PUBLICATION_PUBLISHED.equalsIgnoreCase(story.getPublicationState())
                && story.getArchivedAt() == null;
    }

    private boolean isVisibleComment(StoryComment comment) {
        return comment.getDeletedAt() == null
                && VISIBILITY_VISIBLE.equalsIgnoreCase(comment.getVisibilityState());
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        String accessLevel = user.getAccessLevel().name();
        return ROLE_MODERATOR.equalsIgnoreCase(accessLevel)
                || ROLE_ADMIN.equalsIgnoreCase(accessLevel);
    }

    private AppUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException(MSG_NOT_AUTHENTICATED);
        }

        return appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private AppUser tryGetAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                return appUserRepository.findById(userDetails.getId()).orElse(null);
            }
        } catch (RuntimeException ignored) {
            // Si el contexto de seguridad no está disponible, se trata como usuario anónimo.
        }

        return null;
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && SORT_DESC.equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case SORT_UPDATED_AT -> SORT_UPDATED_AT;
            case SORT_EDITED_AT -> SORT_EDITED_AT;
            default -> SORT_CREATED_AT;
        };
    }
}