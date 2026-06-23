package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateSkillRequest;
import com.nunclear.escritores.dto.request.UpdateSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Skill;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.SkillRepository;
import com.nunclear.escritores.repository.StoryRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private SkillService skillService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSkill_deberiaCrearHabilidad() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Skill saved = mock(Skill.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(skillRepository.save(any(Skill.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getName()).thenReturn("Magia");
        when(saved.getCategoryName()).thenReturn("Combate");
        when(saved.getLevelValue()).thenReturn(8);

        CreateSkillResponse response = skillService.createSkill(
                new CreateSkillRequest(10, "Magia", "Desc", "Combate", 8)
        );

        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Magia", response.name());

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(captor.capture());
        assertEquals("Magia", captor.getValue().getName());
        assertEquals("Combate", captor.getValue().getCategoryName());
    }

    @Test
    void getSkillById_deberiaRetornarSkill_siHistoriaEsPublica() {
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);

        when(skillRepository.findById(5)).thenReturn(Optional.of(skill));
        when(skill.getId()).thenReturn(5);
        when(skill.getStoryId()).thenReturn(10);
        when(skill.getName()).thenReturn("Magia");
        when(skill.getCategoryName()).thenReturn("Combate");
        when(skill.getLevelValue()).thenReturn(8);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        SkillDetailResponse response = skillService.getSkillById(5);

        assertEquals(5, response.id());
        assertEquals("Magia", response.name());
    }

    @Test
    void getSkillById_deberiaLanzarNotFound_siHistoriaNoVisible() {
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);

        when(skillRepository.findById(5)).thenReturn(Optional.of(skill));
        when(skill.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> skillService.getSkillById(5)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getSkillsByStory_deberiaRetornarPagina() {
        Story story = mock(Story.class);
        Skill skill = mock(Skill.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<Skill> page = new PageImpl<>(List.of(skill), PageRequest.of(0, 20), 1);
        when(skillRepository.findByStoryWithFilters(eq(10), eq("Combate"), any(Pageable.class)))
                .thenReturn(page);

        when(skill.getId()).thenReturn(1);
        when(skill.getName()).thenReturn("Magia");
        when(skill.getCategoryName()).thenReturn("Combate");

        PageResponse<SkillListItemResponse> response =
                skillService.getSkillsByStory(10, "Combate", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Magia", response.content().get(0).name());
    }

    @Test
    void searchSkills_deberiaRetornarSoloSkillsVisibles() {
        Skill visible = mock(Skill.class);
        Skill hidden = mock(Skill.class);
        Story publicStory = mock(Story.class);
        Story privateStory = mock(Story.class);

        Page<Skill> page = new PageImpl<>(List.of(visible, hidden), PageRequest.of(0, 20), 2);
        when(skillRepository.searchByName(eq("mag"), any(Pageable.class))).thenReturn(page);

        when(visible.getId()).thenReturn(1);
        when(visible.getName()).thenReturn("Magia");
        when(visible.getStoryId()).thenReturn(10);

        when(hidden.getId()).thenReturn(2);
        when(hidden.getName()).thenReturn("Magia oscura");
        when(hidden.getStoryId()).thenReturn(20);

        when(storyRepository.findById(10)).thenReturn(Optional.of(publicStory));
        when(publicStory.getVisibilityState()).thenReturn("public");
        when(publicStory.getPublicationState()).thenReturn("published");
        when(publicStory.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(20)).thenReturn(Optional.of(privateStory));
        when(privateStory.getVisibilityState()).thenReturn("private");
        when(privateStory.getPublicationState()).thenReturn("draft");
        when(privateStory.getArchivedAt()).thenReturn(null);

        PageResponse<SkillSearchItemResponse> response = skillService.searchSkills("mag", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Magia", response.content().get(0).name());
        assertEquals(2, response.totalElements());
    }

    @Test
    void updateSkill_deberiaActualizarSkill() {
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Skill saved = mock(Skill.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(skillRepository.findById(5)).thenReturn(Optional.of(skill));
        when(skill.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(skillRepository.save(skill)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getName()).thenReturn("Nueva magia");

        UpdateSkillResponse response = skillService.updateSkill(
                5, new UpdateSkillRequest("Nueva magia", "Desc", "Mental", 9)
        );

        assertEquals(5, response.id());
        assertEquals("Nueva magia", response.name());

        verify(skill).setName("Nueva magia");
        verify(skill).setDescription("Desc");
        verify(skill).setCategoryName("Mental");
        verify(skill).setLevelValue(9);
    }

    @Test
    void deleteSkill_deberiaEliminarSkill() {
        Skill skill = mock(Skill.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(skillRepository.findById(5)).thenReturn(Optional.of(skill));
        when(skill.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        MessageResponse response = skillService.deleteSkill(5);

        assertEquals("Habilidad eliminada correctamente", response.message());
        verify(skillRepository).delete(skill);
    }

    @Test
    void createSkill_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> skillService.createSkill(new CreateSkillRequest(10, "Magia", null, null, null))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}