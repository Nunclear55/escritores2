package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.SanctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sanctions")
@RequiredArgsConstructor
public class SanctionController {

    private final SanctionService sanctionService;

    // [137]
    @PostMapping("/warning")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public SanctionResponse createWarning(@Valid @RequestBody CreateWarningRequest request) {
        return sanctionService.createWarning(request);
    }

    // [138]
    @PostMapping("/temporary-ban")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public SanctionResponse createTemporaryBan(@Valid @RequestBody CreateTemporaryBanRequest request) {
        return sanctionService.createTemporaryBan(request);
    }

    // [139]
    @PostMapping("/permanent-ban")
    @PreAuthorize("hasRole('ADMIN')")
    public SanctionResponse createPermanentBan(@Valid @RequestBody CreatePermanentBanRequest request) {
        return sanctionService.createPermanentBan(request);
    }

    // [140]
    @PostMapping("/{id}/lift")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public SanctionResponse liftSanction(
            @PathVariable Integer id,
            @Valid @RequestBody LiftSanctionRequest request
    ) {
        return sanctionService.liftSanction(id, request);
    }

    // [141]
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<SanctionListItemResponse> getSanctionsByUser(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return sanctionService.getSanctionsByUser(userId, page, size, sort);
    }

    // [142]
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<SanctionListItemResponse> getMySanctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return sanctionService.getMySanctions(page, size, sort);
    }

    // [143]
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<SanctionListItemResponse> getActiveSanctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return sanctionService.getActiveSanctions(page, size, sort);
    }
}