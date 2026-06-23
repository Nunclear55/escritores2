package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.AssignCharacterSkillRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharacterSkillService {

    private static final String CHARACTER_NOT_FOUND = "Personaje no encontrado";
    private static final String STORY_NOT_FOUND = "Historia no encontrada";

    private final CharacterSkillRepository characterSkillRepository;
    private final StoryCharacterRepository storyCharacterRepository;
    private final SkillRepository skillRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public AssignCharacterSkillResponse assignSkill(AssignCharacterSkillRequest request) {
        StoryCharacter character = storyCharacterRepository.findById(request.storyCharacterId())
                .orElseThrow(() -> new ResourceNotFoundException(CHARACTER_NOT_FOUND));

        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Habilidad no encontrada"));

        if (!character.getStoryId().equals(skill.getStoryId())) {
            throw new BadRequestException("El personaje y la habilidad deben pertenecer a la misma historia");
        }

        getEditableStory(character.getStoryId());

        if (characterSkillRepository.existsByStoryCharacterIdAndSkillId(character.getId(), skill.getId())) {
            throw new BadRequestException("La habilidad ya está asignada al personaje");
        }

        CharacterSkill relation = new CharacterSkill();
        relation.setStoryCharacterId(character.getId());
        relation.setSkillId(skill.getId());
        relation.setProficiency(request.proficiency());
        relation.setNotes(request.notes());

        CharacterSkill saved = characterSkillRepository.save(relation);

        return new AssignCharacterSkillResponse(
                saved.getId(),
                saved.getStoryCharacterId(),
                saved.getSkillId(),
                saved.getProficiency(),
                saved.getNotes()
        );
    }

    public PageResponse<CharacterSkillForCharacterResponse> getSkillsByCharacter(
            Integer storyCharacterId, int page, int size, String sort
    ) {
        StoryCharacter character = storyCharacterRepository.findById(storyCharacterId)
                .orElseThrow(() -> new ResourceNotFoundException(CHARACTER_NOT_FOUND));

        Story story = storyRepository.findById(character.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<CharacterSkill> result = characterSkillRepository.findByStoryCharacterId(storyCharacterId, pageable);

        var content = result.getContent().stream()
                .map(rel -> {
                    Skill skill = skillRepository.findById(rel.getSkillId()).orElse(null);
                    return new CharacterSkillForCharacterResponse(
                            rel.getId(),
                            rel.getSkillId(),
                            skill != null ? skill.getName() : null,
                            rel.getProficiency(),
                            rel.getNotes()
                    );
                })
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public PageResponse<CharacterSkillForSkillResponse> getCharactersBySkill(
            Integer skillId, int page, int size, String sort
    ) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Habilidad no encontrada"));

        Story story = storyRepository.findById(skill.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<CharacterSkill> result = characterSkillRepository.findBySkillId(skillId, pageable);

        var content = result.getContent().stream()
                .map(rel -> {
                    StoryCharacter character = storyCharacterRepository.findById(rel.getStoryCharacterId()).orElse(null);
                    return new CharacterSkillForSkillResponse(
                            rel.getId(),
                            rel.getStoryCharacterId(),
                            character != null ? character.getName() : null,
                            rel.getProficiency()
                    );
                })
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public UpdateCharacterSkillResponse updateRelation(Integer id, UpdateCharacterSkillRequest request) {
        CharacterSkill relation = getEditableRelation(id);

        relation.setProficiency(request.proficiency());
        relation.setNotes(request.notes());

        CharacterSkill saved = characterSkillRepository.save(relation);

        return new UpdateCharacterSkillResponse(
                saved.getId(),
                saved.getProficiency(),
                saved.getNotes()
        );
    }

    public MessageResponse deleteRelation(Integer id) {
        CharacterSkill relation = getEditableRelation(id);
        characterSkillRepository.delete(relation);
        return new MessageResponse("Relación eliminada correctamente");
    }

    private CharacterSkill getEditableRelation(Integer id) {
        CharacterSkill relation = characterSkillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada"));

        StoryCharacter character = storyCharacterRepository.findById(relation.getStoryCharacterId())
                .orElseThrow(() -> new ResourceNotFoundException(CHARACTER_NOT_FOUND));

        getEditableStory(character.getStoryId());
        return relation;
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
        if (!canReadStory(story)) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }
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

        return story.getOwnerUserId().equals(currentUser.getId()) || isModeratorOrAdmin(currentUser);
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

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "updatedAt" -> "updatedAt";
            case "proficiency" -> "proficiency";
            default -> "createdAt";
        };
    }
}