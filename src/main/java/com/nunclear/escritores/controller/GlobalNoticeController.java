package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateGlobalNoticeRequest;
import com.nunclear.escritores.dto.request.UpdateGlobalNoticeRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.GlobalNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/global-notices")
@RequiredArgsConstructor
public class GlobalNoticeController {

    private final GlobalNoticeService globalNoticeService;

    // [144]
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalNoticeResponse createNotice(@Valid @RequestBody CreateGlobalNoticeRequest request) {
        return globalNoticeService.createNotice(request);
    }

    // [145]
    @GetMapping("/{id}")
    public GlobalNoticeResponse getNoticeById(@PathVariable Integer id) {
        return globalNoticeService.getNoticeById(id);
    }

    // [146]
    @GetMapping("/active")
    public PageResponse<GlobalNoticeListItemResponse> getActiveNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return globalNoticeService.getActiveNotices(page, size, sort);
    }

    // [147]
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public PageResponse<GlobalNoticeListItemResponse> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return globalNoticeService.getHistory(page, size, sort);
    }

    // [148]
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalNoticeResponse updateNotice(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateGlobalNoticeRequest request
    ) {
        return globalNoticeService.updateNotice(id, request);
    }

    // [149]
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalNoticeToggleResponse enableNotice(@PathVariable Integer id) {
        return globalNoticeService.enableNotice(id);
    }

    // [150]
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalNoticeToggleResponse disableNotice(@PathVariable Integer id) {
        return globalNoticeService.disableNotice(id);
    }

    // [151]
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalNoticeArchiveResponse archiveNotice(@PathVariable Integer id) {
        return globalNoticeService.archiveNotice(id);
    }
}