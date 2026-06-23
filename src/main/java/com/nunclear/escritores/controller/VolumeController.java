package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.VolumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/volumes")
@RequiredArgsConstructor
public class VolumeController {

    private final VolumeService volumeService;

    // [60]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateVolumeResponse createVolume(@Valid @RequestBody CreateVolumeRequest request) {
        return volumeService.createVolume(request);
    }

    // [61]
    @GetMapping("/{id}")
    public VolumeDetailResponse getVolumeById(@PathVariable Integer id) {
        return volumeService.getVolumeById(id);
    }

    // [62]
    @GetMapping("/story/{storyId}")
    public PageResponse<VolumeListItemResponse> getVolumesByStory(
            @PathVariable Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return volumeService.getVolumesByStory(storyId, page, size, sort);
    }

    // [63]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateVolumeResponse updateVolume(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateVolumeRequest request
    ) {
        return volumeService.updateVolume(id, request);
    }

    // [64]
    @PostMapping("/reorder")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse reorderVolumes(@Valid @RequestBody ReorderVolumesRequest request) {
        return volumeService.reorderVolumes(request);
    }

    // [65]
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MoveVolumeResponse moveVolume(
            @PathVariable Integer id,
            @Valid @RequestBody MoveVolumeRequest request
    ) {
        return volumeService.moveVolume(id, request);
    }

    // [66]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteVolume(@PathVariable Integer id) {
        return volumeService.deleteVolume(id);
    }
}