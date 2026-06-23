package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.AssignCharacterSkillRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.CharacterSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/character-skills")
@RequiredArgsConstructor
public class CharacterSkillController {

    private final CharacterSkillService characterSkillService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public AssignCharacterSkillResponse assignSkill(@Valid @RequestBody AssignCharacterSkillRequest request) {
        return characterSkillService.assignSkill(request);
    }

    @GetMapping("/character/{storyCharacterId}")
    public PageResponse<CharacterSkillForCharacterResponse> getSkillsByCharacter(
            @PathVariable Integer storyCharacterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return characterSkillService.getSkillsByCharacter(storyCharacterId, page, size, sort);
    }

    @GetMapping("/skill/{skillId}")
    public PageResponse<CharacterSkillForSkillResponse> getCharactersBySkill(
            @PathVariable Integer skillId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return characterSkillService.getCharactersBySkill(skillId, page, size, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateCharacterSkillResponse updateRelation(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCharacterSkillRequest request
    ) {
        return characterSkillService.updateRelation(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteRelation(@PathVariable Integer id) {
        return characterSkillService.deleteRelation(id);
    }
}