package com.nunclear.escritores.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateEventRequest;
import com.nunclear.escritores.dto.request.UpdateEventRequest;
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

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String STORY_NOT_FOUND = "Historia no encontrada";
    private static final String PUBLICATION_STATE_PUBLISHED = "published";
    private static final String VISIBILITY_STATE_PUBLIC = "public";
    private static final String SORT_EVENT_ON = "eventOn";
    private static final String DEFAULT_EVENT_SORT = SORT_EVENT_ON + ",desc";

    private final StoryEventRepository storyEventRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryCharacterRepository storyCharacterRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateEventResponse createEvent(CreateEventRequest request) {
        Story story = getEditableStory(request.storyId());

        validateChapterBelongsToStory(request.chapterId(), story.getId());
        validateLinkedCharactersBelongToStory(request.linkedCharacterIds(), story.getId());

        StoryEvent event = new StoryEvent();
        event.setStoryId(story.getId());
        event.setChapterId(request.chapterId());
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventOn(request.eventOn());
        event.setImportance(request.importance());
        event.setEventKind(request.eventKind());
        event.setTagsJson(toJson(request.tagsJson()));
        event.setLinkedCharactersJson(toJson(request.linkedCharacterIds()));

        StoryEvent saved = storyEventRepository.save(event);

        return new CreateEventResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getChapterId(),
                saved.getTitle()
        );
    }

    public EventDetailResponse getEventById(Integer id) {
        StoryEvent event = storyEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        Story story = storyRepository.findById(event.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getImportance(),
                event.getEventKind()
        );
    }

    public PageResponse<EventListItemResponse> getEventsByStory(
            Integer storyId, String eventKind, Integer importance, int page, int size, String sort
    ) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_EVENT_SORT : sort);
        Page<StoryEvent> result = storyEventRepository.findByStoryWithFilters(storyId, eventKind, importance, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(event -> new EventListItemResponse(
                                event.getId(),
                                event.getTitle(),
                                event.getChapterId()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<EventListItemResponse> getEventsByChapter(Integer chapterId, int page, int size, String sort) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Capítulo no encontrado"));

        Story story = storyRepository.findById(chapter.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateChapterReadAccess(chapter, story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_EVENT_SORT : sort);
        Page<StoryEvent> result = storyEventRepository.findByChapterId(chapterId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(event -> new EventListItemResponse(
                                event.getId(),
                                event.getTitle(),
                                event.getChapterId()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<EventListItemResponse> searchEvents(
            String q, String tag, int page, int size, String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_EVENT_SORT : sort);
        Page<StoryEvent> result = storyEventRepository.searchEvents(q, tag, pageable);

        var content = result.getContent().stream()
                .filter(event -> {
                    Story story = storyRepository.findById(event.getStoryId()).orElse(null);
                    return story != null && canReadStory(story);
                })
                .map(event -> new EventListItemResponse(
                        event.getId(),
                        event.getTitle(),
                        event.getChapterId()
                ))
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public UpdateEventResponse updateEvent(Integer id, UpdateEventRequest request) {
        StoryEvent event = getEditableEvent(id);

        validateLinkedCharactersBelongToStory(request.linkedCharacterIds(), event.getStoryId());

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventOn(request.eventOn());
        event.setImportance(request.importance());
        event.setEventKind(request.eventKind());
        event.setTagsJson(toJson(request.tagsJson()));
        event.setLinkedCharactersJson(toJson(request.linkedCharacterIds()));

        StoryEvent saved = storyEventRepository.save(event);

        return new UpdateEventResponse(saved.getId(), saved.getTitle());
    }

    public MessageResponse deleteEvent(Integer id) {
        StoryEvent event = getEditableEvent(id);
        storyEventRepository.delete(event);
        return new MessageResponse("Evento eliminado correctamente");
    }

    private StoryEvent getEditableEvent(Integer id) {
        StoryEvent event = storyEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        getEditableStory(event.getStoryId());
        return event;
    }

    private Story getEditableStory(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        AppUser currentUser = getAuthenticatedUser();
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre esta historia");
        }

        return story;
    }

    private void validateChapterBelongsToStory(Integer chapterId, Integer storyId) {
        if (chapterId == null) {
            return;
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BadRequestException("El capítulo no existe"));

        if (!chapter.getStoryId().equals(storyId)) {
            throw new BadRequestException("El capítulo no pertenece a la historia");
        }
    }

    private void validateLinkedCharactersBelongToStory(java.util.List<Integer> characterIds, Integer storyId) {
        if (characterIds == null || characterIds.isEmpty()) {
            return;
        }

        for (Integer characterId : characterIds) {
            StoryCharacter character = storyCharacterRepository.findById(characterId)
                    .orElseThrow(() -> new BadRequestException("Uno de los personajes enlazados no existe"));

            if (!character.getStoryId().equals(storyId)) {
                throw new BadRequestException("Todos los personajes enlazados deben pertenecer a la historia");
            }
        }
    }

    private void validateReadAccess(Story story) {
        if (!canReadStory(story)) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }
    }

    private void validateChapterReadAccess(Chapter chapter, Story story) {
        boolean publicReadable =
                chapter.getArchivedAt() == null
                        && PUBLICATION_STATE_PUBLISHED.equalsIgnoreCase(chapter.getPublicationState())
                        && VISIBILITY_STATE_PUBLIC.equalsIgnoreCase(story.getVisibilityState())
                        && PUBLICATION_STATE_PUBLISHED.equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (!publicReadable && !canReadStory(story)) {
            throw new ResourceNotFoundException("Capítulo no encontrado");
        }
    }

    private boolean canReadStory(Story story) {
        boolean publicReadable =
                VISIBILITY_STATE_PUBLIC.equalsIgnoreCase(story.getVisibilityState())
                        && PUBLICATION_STATE_PUBLISHED.equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return true;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            return false;
        }

        return story.getOwnerUserId().equals(currentUser.getId()) || isModeratorOrAdmin(currentUser);
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

    private String toJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("JSON inválido");
        }
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
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "title" -> "title";
            case SORT_EVENT_ON -> SORT_EVENT_ON;
            default -> SORT_EVENT_ON;
        };
    }
}