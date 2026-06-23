package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateSkillRequest;
import com.nunclear.escritores.dto.request.UpdateSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateSkillResponse createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return skillService.createSkill(request);
    }

    @GetMapping("/{id}")
    public SkillDetailResponse getSkillById(@PathVariable Integer id) {
        return skillService.getSkillById(id);
    }

    @GetMapping("/story/{storyId}")
    public PageResponse<SkillListItemResponse> getSkillsByStory(
            @PathVariable Integer storyId,
            @RequestParam(required = false) String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return skillService.getSkillsByStory(storyId, categoryName, page, size, sort);
    }

    @GetMapping("/search")
    public PageResponse<SkillSearchItemResponse> searchSkills(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return skillService.searchSkills(q, page, size, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateSkillResponse updateSkill(@PathVariable Integer id, @Valid @RequestBody UpdateSkillRequest request) {
        return skillService.updateSkill(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteSkill(@PathVariable Integer id) {
        return skillService.deleteSkill(id);
    }
}