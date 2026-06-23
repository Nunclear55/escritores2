package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateCharacterRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    // [67]
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateCharacterResponse createCharacter(@Valid @RequestBody CreateCharacterRequest request) {
        return characterService.createCharacter(request);
    }

    // [68]
    @GetMapping("/{id}")
    public CharacterDetailResponse getCharacterById(@PathVariable Integer id) {
        return characterService.getCharacterById(id);
    }

    // [69]
    @GetMapping("/story/{storyId}")
    public PageResponse<CharacterListItemResponse> getCharactersByStory(
            @PathVariable Integer storyId,
            @RequestParam(required = false) Boolean isAlive,
            @RequestParam(required = false) String characterRoleName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return characterService.getCharactersByStory(storyId, isAlive, characterRoleName, page, size, sort);
    }

    // [70]
    @GetMapping("/search")
    public PageResponse<CharacterSearchItemResponse> searchCharacters(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return characterService.searchCharacters(q, page, size, sort);
    }

    // [71]
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateCharacterResponse updateCharacter(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCharacterRequest request
    ) {
        return characterService.updateCharacter(id, request);
    }

    // [72]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteCharacter(@PathVariable Integer id) {
        return characterService.deleteCharacter(id);
    }
}