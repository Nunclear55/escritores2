package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.UpdateUserAccessLevelRequest;
import com.nunclear.escritores.dto.request.UpdateUserAccountStateRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // [21]
    @PatchMapping("/{id}/access-level")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserAccessLevelResponse updateAccessLevel(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserAccessLevelRequest request
    ) {
        return adminUserService.updateAccessLevel(id, request);
    }

    // [22]
    @PatchMapping("/{id}/account-state")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserAccountStateResponse updateAccountState(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserAccountStateRequest request
    ) {
        return adminUserService.updateAccountState(id, request);
    }

    // [23]
    @GetMapping("/by-role")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AdminUserByRoleItemResponse> listUsersByRole(
            @RequestParam String accessLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return adminUserService.listUsersByRole(accessLevel, page, size, sort);
    }

    // [24]
    @GetMapping("/by-state")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<AdminUserByStateItemResponse> listUsersByState(
            @RequestParam String accountState,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return adminUserService.listUsersByState(accountState, page, size, sort);
    }

    // [25]
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public UserHistoryResponse getUserHistory(@PathVariable Integer id) {
        return adminUserService.getUserHistory(id);
    }
}