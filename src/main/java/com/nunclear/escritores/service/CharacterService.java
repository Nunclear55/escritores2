package com.nunclear.escritores.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateCharacterRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.StoryCharacter;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryCharacterRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final String STORY_NOT_FOUND = "Historia no encontrada";

    private final StoryCharacterRepository storyCharacterRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateCharacterResponse createCharacter(CreateCharacterRequest request) {
        Story story = getEditableStory(request.storyId());

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
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

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
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort);
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
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort);
        Page<StoryCharacter> result = storyCharacterRepository.searchByName(q == null ? "" : q, pageable);

        var visibleCharacters = result.getContent().stream()
                .filter(character -> {
                    Story story = storyRepository.findById(character.getStoryId()).orElse(null);
                    if (story == null) {
                        return false;
                    }
                    return canReadStory(story);
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

        getEditableStory(character.getStoryId());
        return character;
    }

    private Story getEditableStory(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        AppUser currentUser = getAuthenticatedUser();
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre esta historia");
        }

        return story;
    }

    private void validateReadAccess(Story story) {
        if (canReadStory(story)) {
            return;
        }
        throw new ResourceNotFoundException(STORY_NOT_FOUND);
    }

    private boolean canReadStory(Story story) {
        boolean publicReadable =
                "public".equalsIgnoreCase(story.getVisibilityState())
                        && "published".equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return true;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            return false;
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        return isOwner || isModeratorOrAdmin;
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("No autenticado");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No autenticado");
        }

        return appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private AppUser tryGetAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return appUserRepository.findById(userDetails.getId()).orElse(null);
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("rolesJson inválido");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "name" -> "name";
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "characterRoleName" -> "characterRoleName";
            default -> "name";
        };
    }
}