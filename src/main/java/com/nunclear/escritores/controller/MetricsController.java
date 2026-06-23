package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.RegisterChapterViewRequest;
import com.nunclear.escritores.dto.request.RegisterStoryViewRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    // [163]
    @PostMapping("/views/story")
    public MessageResponse registerStoryView(@Valid @RequestBody RegisterStoryViewRequest request) {
        return metricsService.registerStoryView(request);
    }

    // [164]
    @PostMapping("/views/chapter")
    public MessageResponse registerChapterView(@Valid @RequestBody RegisterChapterViewRequest request) {
        return metricsService.registerChapterView(request);
    }

    // [165]
    @GetMapping("/story/{storyId}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public StoryMetricsResponse getStoryMetrics(@PathVariable Integer storyId) {
        return metricsService.getStoryMetrics(storyId);
    }

    // [166]
    @GetMapping("/chapter/{chapterId}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ChapterMetricsResponse getChapterMetrics(@PathVariable Integer chapterId) {
        return metricsService.getChapterMetrics(chapterId);
    }

    // [167]
    @GetMapping("/author/{userId}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public AuthorMetricsResponse getAuthorMetrics(@PathVariable Integer userId) {
        return metricsService.getAuthorMetrics(userId);
    }

    // [168]
    @GetMapping("/stories/top-viewed")
    public PageResponse<TopViewedStoryItemResponse> getTopViewedStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return metricsService.getTopViewedStories(page, size);
    }
}