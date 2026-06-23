package com.nunclear.escritores.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateCharacterRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.StoryCharacter;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryCharacterRepository;
import com.nunclear.escritores.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final StoryCharacterRepository storyCharacterRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateCharacterResponse createCharacter(CreateCharacterRequest request) {
        Story story = StoryAccessUtils.getEditableStory(request.storyId(), storyRepository, appUserRepository);

        StoryCharacter character = new StoryCharacter();
        character.setStoryId(story.getId());
        character.setName(request.name());
        character.setDescription(request.description());
        character.setCharacterRoleName(request.characterRoleName());
        character.setProfession(request.profession());
        character.setAbility(request.ability());
        character.setAge(request.age());
        character.setBirthDate(request.birthDate());
        character.setIsAlive(request.isAlive());
        character.setRolesJson(toJson(request.rolesJson()));
        character.setImageUrl(request.imageUrl());

        StoryCharacter saved = storyCharacterRepository.save(character);

        return new CreateCharacterResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getName(),
                saved.getCharacterRoleName()
        );
    }

    public CharacterDetailResponse getCharacterById(Integer id) {
        StoryCharacter character = storyCharacterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personaje no encontrado"));

        Story story = storyRepository.findById(character.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        return new CharacterDetailResponse(
                character.getId(),
                character.getStoryId(),
                character.getName(),
                character.getDescription(),
                character.getCharacterRoleName(),
                character.getProfession()
        );
    }

    public PageResponse<CharacterListItemResponse> getCharactersByStory(
            Integer storyId,
            Boolean isAlive,
            String characterRoleName,
            int page,
            int size,
            String sort
    ) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort, "name", "createdAt", "updatedAt", "characterRoleName");
        Page<StoryCharacter> result = storyCharacterRepository.findByStoryWithFilters(
                storyId,
                isAlive,
                characterRoleName,
                pageable
        );

        return new PageResponse<>(
                result.getContent().stream()
                        .map(character -> new CharacterListItemResponse(
                                character.getId(),
                                character.getName(),
                                character.getCharacterRoleName()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<CharacterSearchItemResponse> searchCharacters(
            String q,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort, "name", "createdAt", "updatedAt", "characterRoleName");
        Page<StoryCharacter> result = storyCharacterRepository.searchByName(q == null ? "" : q, pageable);

        var visibleCharacters = result.getContent().stream()
                .filter(character -> {
                    Story story = storyRepository.findById(character.getStoryId()).orElse(null);
                    if (story == null) {
                        return false;
                    }
                    return StoryAccessUtils.canReadStory(story, appUserRepository);
                })
                .map(character -> new CharacterSearchItemResponse(
                        character.getId(),
                        character.getName(),
                        character.getStoryId()
                ))
                .toList();

        return new PageResponse<>(
                visibleCharacters,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateCharacterResponse updateCharacter(Integer id, UpdateCharacterRequest request) {
        StoryCharacter character = getEditableCharacter(id);

        character.setName(request.name());
        character.setDescription(request.description());
        character.setCharacterRoleName(request.characterRoleName());
        character.setProfession(request.profession());
        character.setAbility(request.ability());
        character.setAge(request.age());
        character.setBirthDate(request.birthDate());
        character.setIsAlive(request.isAlive());
        character.setRolesJson(toJson(request.rolesJson()));
        character.setImageUrl(request.imageUrl());

        StoryCharacter saved = storyCharacterRepository.save(character);

        return new UpdateCharacterResponse(
                saved.getId(),
                saved.getName(),
                saved.getUpdatedAt()
        );
    }

    public MessageResponse deleteCharacter(Integer id) {
        StoryCharacter character = getEditableCharacter(id);
        storyCharacterRepository.delete(character);
        return new MessageResponse("Personaje eliminado correctamente");
    }

    private StoryCharacter getEditableCharacter(Integer id) {
        StoryCharacter character = storyCharacterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personaje no encontrado"));

        StoryAccessUtils.getEditableStory(character.getStoryId(), storyRepository, appUserRepository);
        return character;
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("rolesJson inválido");
        }
    }

}