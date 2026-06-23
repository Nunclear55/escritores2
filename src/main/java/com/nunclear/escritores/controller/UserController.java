package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // [10]
    @GetMapping("/{id}")
    public UserProfileResponse getUserById(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    // [11]
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CurrentUserResponse getMyProfile() {
        return userService.getMyProfile();
    }

    // [12]
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public PageResponse<UserListItemResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return userService.listUsers(page, size, sort);
    }

    // [13]
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<UserSearchItemResponse> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return userService.searchUsers(q, page, size, sort);
    }

    // [14]
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateMyProfileResponse updateMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return userService.updateMyProfile(request);
    }

    // [15]
    @PatchMapping("/me/avatar")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public AvatarResponse changeAvatar(@Valid @RequestBody ChangeAvatarRequest request) {
        return userService.changeAvatar(request);
    }

    // [16]
    @PostMapping("/me/change-password")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(request);
    }

    // [17]
    @PostMapping("/me/change-email")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ChangeEmailResponse changeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        return userService.changeEmail(request);
    }

    // [18]
    @PostMapping("/me/deactivate")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deactivateMyAccount() {
        return userService.deactivateMyAccount();
    }

    // [19]
    @GetMapping("/{id}/public-profile")
    public PublicAuthorProfileResponse getPublicAuthorProfile(@PathVariable Integer id) {
        return userService.getPublicAuthorProfile(id);
    }

    // [20]
    @GetMapping("/{id}/stories")
    public PageResponse<UserStoryItemResponse> getPublicStoriesByAuthor(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return userService.getPublicStoriesByAuthor(id, page, size, sort);
    }
}