package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateFavoriteRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // [152]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public FavoriteResponse createFavorite(@Valid @RequestBody CreateFavoriteRequest request) {
        return favoriteService.createFavorite(request);
    }

    // [153]
    @DeleteMapping("/{storyId}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse removeFavorite(@PathVariable Integer storyId) {
        return favoriteService.removeFavorite(storyId);
    }

    // [154]
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public PageResponse<FavoriteListItemResponse> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return favoriteService.getMyFavorites(page, size, sort);
    }

    // [155]
    @GetMapping("/story/{storyId}/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public FavoriteCheckResponse isFavorite(@PathVariable Integer storyId) {
        return favoriteService.isFavorite(storyId);
    }

    // [156]
    @GetMapping("/story/{storyId}/count")
    public FavoriteCountResponse countFavorites(@PathVariable Integer storyId) {
        return favoriteService.countFavorites(storyId);
    }
}