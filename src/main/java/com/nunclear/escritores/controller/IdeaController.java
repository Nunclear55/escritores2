package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateIdeaRequest;
import com.nunclear.escritores.dto.request.UpdateIdeaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.IdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ideas")
@RequiredArgsConstructor
public class IdeaController {

    private final IdeaService ideaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateIdeaResponse createIdea(@Valid @RequestBody CreateIdeaRequest request) {
        return ideaService.createIdea(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public IdeaDetailResponse getIdeaById(@PathVariable Integer id) {
        return ideaService.getIdeaById(id);
    }

    @GetMapping("/story/{storyId}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<IdeaListItemResponse> getIdeasByStory(
            @PathVariable Integer storyId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ideaService.getIdeasByStory(storyId, q, page, size, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateIdeaResponse updateIdea(@PathVariable Integer id, @Valid @RequestBody UpdateIdeaRequest request) {
        return ideaService.updateIdea(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteIdea(@PathVariable Integer id) {
        return ideaService.deleteIdea(id);
    }
}