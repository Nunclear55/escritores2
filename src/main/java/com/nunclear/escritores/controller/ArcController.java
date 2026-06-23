package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateArcRequest;
import com.nunclear.escritores.dto.request.ReorderArcsRequest;
import com.nunclear.escritores.dto.request.UpdateArcRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.ArcService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/arcs")
@RequiredArgsConstructor
public class ArcController {

    private final ArcService arcService;

    // [54]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateArcResponse createArc(@Valid @RequestBody CreateArcRequest request) {
        return arcService.createArc(request);
    }

    // [55]
    @GetMapping("/{id}")
    public ArcDetailResponse getArcById(@PathVariable Integer id) {
        return arcService.getArcById(id);
    }

    // [56]
    @GetMapping("/story/{storyId}")
    public PageResponse<ArcListItemResponse> getArcsByStory(
            @PathVariable Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return arcService.getArcsByStory(storyId, page, size, sort);
    }

    // [57]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateArcResponse updateArc(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateArcRequest request
    ) {
        return arcService.updateArc(id, request);
    }

    // [58]
    @PostMapping("/reorder")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse reorderArcs(@Valid @RequestBody ReorderArcsRequest request) {
        return arcService.reorderArcs(request);
    }

    // [59]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteArc(@PathVariable Integer id) {
        return arcService.deleteArc(id);
    }
}