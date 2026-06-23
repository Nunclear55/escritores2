package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.UpsertRatingRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // [119]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public RatingResponse upsertRating(@Valid @RequestBody UpsertRatingRequest request) {
        return ratingService.upsertRating(request);
    }

    // [120]
    @GetMapping("/{id}")
    public RatingResponse getRatingById(@PathVariable Integer id) {
        return ratingService.getRatingById(id);
    }

    // [121]
    @GetMapping("/story/{storyId}")
    public PageResponse<RatingListItemResponse> getRatingsByStory(
            @PathVariable Integer storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ratingService.getRatingsByStory(storyId, page, size, sort);
    }

    // [122]
    @GetMapping("/story/{storyId}/average")
    public RatingAverageResponse getAverageByStory(@PathVariable Integer storyId) {
        return ratingService.getAverageByStory(storyId);
    }

    // [123]
    @GetMapping("/story/{storyId}/me")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public RatingResponse getMyRating(@PathVariable Integer storyId) {
        return ratingService.getMyRating(storyId);
    }

    // [124]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteRating(@PathVariable Integer id) {
        return ratingService.deleteRating(id);
    }
}