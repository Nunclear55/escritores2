package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateFollowRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    // [157]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public FollowResponse createFollow(@Valid @RequestBody CreateFollowRequest request) {
        return followService.createFollow(request);
    }

    // [158]
    @DeleteMapping("/{followedUserId}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse unfollow(@PathVariable Integer followedUserId) {
        return followService.unfollow(followedUserId);
    }

    // [159]
    @GetMapping("/me/following")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<FollowUserItemResponse> getMyFollowing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return followService.getMyFollowing(page, size, sort);
    }

    // [160]
    @GetMapping("/user/{userId}/followers")
    public PageResponse<FollowUserItemResponse> getFollowers(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return followService.getFollowers(userId, page, size, sort);
    }

    // [161]
    @GetMapping("/user/{userId}/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public FollowCheckResponse isFollowing(@PathVariable Integer userId) {
        return followService.isFollowing(userId);
    }

    // [162]
    @GetMapping("/user/{userId}/count")
    public FollowCountResponse countFollowers(@PathVariable Integer userId) {
        return followService.countFollowers(userId);
    }
}