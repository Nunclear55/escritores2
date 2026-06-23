package com.nunclear.escritores.service;


import com.nunclear.escritores.dto.request.AssignCharacterSkillRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class CharacterSkillService {

    private static final String CHARACTER_NOT_FOUND = "Personaje no encontrado";

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

        StoryAccessUtils.getEditableStory(character.getStoryId(), storyRepository, appUserRepository);

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "updatedAt", "proficiency");
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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "updatedAt", "proficiency");
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

        StoryAccessUtils.getEditableStory(character.getStoryId(), storyRepository, appUserRepository);
        return relation;
    }

}