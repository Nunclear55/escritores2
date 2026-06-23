package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateEventRequest;
import com.nunclear.escritores.dto.request.UpdateEventRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping("/{id}")
    public EventDetailResponse getEventById(@PathVariable Integer id) {
        return eventService.getEventById(id);
    }

    @GetMapping("/story/{storyId}")
    public PageResponse<EventListItemResponse> getEventsByStory(
            @PathVariable Integer storyId,
            @RequestParam(required = false) String eventKind,
            @RequestParam(required = false) Integer importance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return eventService.getEventsByStory(storyId, eventKind, importance, page, size, sort);
    }

    @GetMapping("/chapter/{chapterId}")
    public PageResponse<EventListItemResponse> getEventsByChapter(
            @PathVariable Integer chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return eventService.getEventsByChapter(chapterId, page, size, sort);
    }

    @GetMapping("/search")
    public PageResponse<EventListItemResponse> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return eventService.searchEvents(q, tag, page, size, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateEventResponse updateEvent(@PathVariable Integer id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteEvent(@PathVariable Integer id) {
        return eventService.deleteEvent(id);
    }
}