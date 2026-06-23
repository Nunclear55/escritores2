package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.AssignCharacterSkillRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CharacterSkillServiceTest {

    @Mock private CharacterSkillRepository characterSkillRepository;
    @Mock private StoryCharacterRepository storyCharacterRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks
    private CharacterSkillService characterSkillService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignSkill_deberiaCrearRelacion() {
        StoryCharacter character = mock(StoryCharacter.class);
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CharacterSkill saved = mock(CharacterSkill.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(skillRepository.findById(22)).thenReturn(Optional.of(skill));

        when(character.getId()).thenReturn(11);
        when(character.getStoryId()).thenReturn(10);

        when(skill.getId()).thenReturn(22);
        when(skill.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(characterSkillRepository.existsByStoryCharacterIdAndSkillId(11, 22)).thenReturn(false);
        when(characterSkillRepository.save(any(CharacterSkill.class))).thenReturn(saved);

        when(saved.getId()).thenReturn(100);
        when(saved.getStoryCharacterId()).thenReturn(11);
        when(saved.getSkillId()).thenReturn(22);
        when(saved.getProficiency()).thenReturn(7);
        when(saved.getNotes()).thenReturn("Nota");

        AssignCharacterSkillResponse response = characterSkillService.assignSkill(
                new AssignCharacterSkillRequest(11, 22, 7, "Nota")
        );

        assertEquals(100, response.id());
        assertEquals(11, response.storyCharacterId());
        assertEquals(22, response.skillId());
    }

    @Test
    void assignSkill_deberiaLanzarBadRequest_siNoSonDeLaMismaHistoria() {
        StoryCharacter character = mock(StoryCharacter.class);
        Skill skill = mock(Skill.class);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(skillRepository.findById(22)).thenReturn(Optional.of(skill));
        when(character.getStoryId()).thenReturn(10);
        when(skill.getStoryId()).thenReturn(20);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> characterSkillService.assignSkill(new AssignCharacterSkillRequest(11, 22, 7, "Nota"))
        );

        assertEquals("El personaje y la habilidad deben pertenecer a la misma historia", ex.getMessage());
    }

    @Test
    void assignSkill_deberiaLanzarBadRequest_siRelacionYaExiste() {
        StoryCharacter character = mock(StoryCharacter.class);
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(skillRepository.findById(22)).thenReturn(Optional.of(skill));
        when(character.getId()).thenReturn(11);
        when(character.getStoryId()).thenReturn(10);
        when(skill.getId()).thenReturn(22);
        when(skill.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(characterSkillRepository.existsByStoryCharacterIdAndSkillId(11, 22)).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> characterSkillService.assignSkill(new AssignCharacterSkillRequest(11, 22, 7, "Nota"))
        );

        assertEquals("La habilidad ya está asignada al personaje", ex.getMessage());
    }

    @Test
    void getSkillsByCharacter_deberiaRetornarPagina() {
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);
        CharacterSkill rel = mock(CharacterSkill.class);
        Skill skill = mock(Skill.class);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<CharacterSkill> page = new PageImpl<>(List.of(rel), PageRequest.of(0, 20), 1);
        when(characterSkillRepository.findByStoryCharacterId(eq(11), any(Pageable.class))).thenReturn(page);

        when(rel.getId()).thenReturn(1);
        when(rel.getSkillId()).thenReturn(22);
        when(rel.getProficiency()).thenReturn(7);
        when(rel.getNotes()).thenReturn("Nota");

        when(skillRepository.findById(22)).thenReturn(Optional.of(skill));
        when(skill.getName()).thenReturn("Magia");

        PageResponse<CharacterSkillForCharacterResponse> response =
                characterSkillService.getSkillsByCharacter(11, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Magia", response.content().get(0).skillName());
    }

    @Test
    void getCharactersBySkill_deberiaRetornarPagina() {
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);
        CharacterSkill rel = mock(CharacterSkill.class);
        StoryCharacter character = mock(StoryCharacter.class);

        when(skillRepository.findById(22)).thenReturn(Optional.of(skill));
        when(skill.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<CharacterSkill> page = new PageImpl<>(List.of(rel), PageRequest.of(0, 20), 1);
        when(characterSkillRepository.findBySkillId(eq(22), any(Pageable.class))).thenReturn(page);

        when(rel.getId()).thenReturn(1);
        when(rel.getStoryCharacterId()).thenReturn(11);
        when(rel.getProficiency()).thenReturn(7);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(character.getName()).thenReturn("Alicia");

        PageResponse<CharacterSkillForSkillResponse> response =
                characterSkillService.getCharactersBySkill(22, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Alicia", response.content().get(0).characterName());
    }

    @Test
    void updateRelation_deberiaActualizarRelacion() {
        CharacterSkill rel = mock(CharacterSkill.class);
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CharacterSkill saved = mock(CharacterSkill.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(characterSkillRepository.findById(5)).thenReturn(Optional.of(rel));
        when(rel.getStoryCharacterId()).thenReturn(11);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(characterSkillRepository.save(rel)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getProficiency()).thenReturn(9);
        when(saved.getNotes()).thenReturn("Actualizado");

        UpdateCharacterSkillResponse response =
                characterSkillService.updateRelation(5, new UpdateCharacterSkillRequest(9, "Actualizado"));

        assertEquals(5, response.id());
        assertEquals(9, response.proficiency());
        assertEquals("Actualizado", response.notes());

        verify(rel).setProficiency(9);
        verify(rel).setNotes("Actualizado");
    }

    @Test
    void deleteRelation_deberiaEliminarRelacion() {
        CharacterSkill rel = mock(CharacterSkill.class);
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(characterSkillRepository.findById(5)).thenReturn(Optional.of(rel));
        when(rel.getStoryCharacterId()).thenReturn(11);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        MessageResponse response = characterSkillService.deleteRelation(5);

        assertEquals("Relación eliminada correctamente", response.message());
        verify(characterSkillRepository).delete(rel);
    }

    @Test
    void assignSkill_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        StoryCharacter character = mock(StoryCharacter.class);
        Skill skill = mock(Skill.class);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(skillRepository.findById(22)).thenReturn(Optional.of(skill));
        when(character.getId()).thenReturn(11);
        when(character.getStoryId()).thenReturn(10);
        when(skill.getId()).thenReturn(22);
        when(skill.getStoryId()).thenReturn(10);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> characterSkillService.assignSkill(new AssignCharacterSkillRequest(11, 22, 7, "Nota"))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}