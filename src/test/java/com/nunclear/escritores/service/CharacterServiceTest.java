package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateCharacterRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.StoryCharacter;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryCharacterRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock
    private StoryCharacterRepository storyCharacterRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CharacterService characterService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCharacter_deberiaCrearPersonaje_siEsOwner() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        StoryCharacter saved = mock(StoryCharacter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(storyCharacterRepository.save(any(StoryCharacter.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getName()).thenReturn("Alicia");
        when(saved.getCharacterRoleName()).thenReturn("protagonist");

        CreateCharacterRequest request = new CreateCharacterRequest(
                10,
                "Alicia",
                "Descripción",
                "protagonist",
                "Guerrera",
                "Magia",
                20,
                LocalDate.of(2005, 1, 1),
                true,
                List.of("hero", "mage"),
                "https://img.test/alicia.jpg"
        );

        CreateCharacterResponse response = characterService.createCharacter(request);

        assertNotNull(response);
        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Alicia", response.name());
        assertEquals("protagonist", response.characterRoleName());

        ArgumentCaptor<StoryCharacter> captor = ArgumentCaptor.forClass(StoryCharacter.class);
        verify(storyCharacterRepository).save(captor.capture());

        StoryCharacter toSave = captor.getValue();
        assertEquals(10, toSave.getStoryId());
        assertEquals("Alicia", toSave.getName());
        assertEquals("Descripción", toSave.getDescription());
        assertEquals("protagonist", toSave.getCharacterRoleName());
        assertEquals("Guerrera", toSave.getProfession());
        assertEquals("Magia", toSave.getAbility());
        assertEquals(20, toSave.getAge());
        assertEquals(LocalDate.of(2005, 1, 1), toSave.getBirthDate());
        assertEquals(true, toSave.getIsAlive());
        assertEquals("[\"hero\",\"mage\"]", toSave.getRolesJson());
        assertEquals("https://img.test/alicia.jpg", toSave.getImageUrl());
    }

    @Test
    void createCharacter_deberiaLanzarUnauthorized_siNoEsOwnerNiAdminNiModerator() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(99);
        mockAuthenticated(principal);

        when(appUserRepository.findById(99)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(99);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> characterService.createCharacter(new CreateCharacterRequest(
                        10, "Alicia", null, null, null, null,
                        null, null, null, null, null
                ))
        );

        assertEquals("No tienes permisos sobre esta historia", ex.getMessage());
        verify(storyCharacterRepository, never()).save(any());
    }

    @Test
    void getCharacterById_deberiaRetornarPersonaje_siHistoriaEsPublica() {
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);

        when(storyCharacterRepository.findById(5)).thenReturn(Optional.of(character));
        when(character.getId()).thenReturn(5);
        when(character.getStoryId()).thenReturn(10);
        when(character.getName()).thenReturn("Alicia");
        when(character.getDescription()).thenReturn("Descripción");
        when(character.getCharacterRoleName()).thenReturn("protagonist");
        when(character.getProfession()).thenReturn("Guerrera");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        CharacterDetailResponse response = characterService.getCharacterById(5);

        assertEquals(5, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Alicia", response.name());
        assertEquals("Descripción", response.description());
        assertEquals("protagonist", response.characterRoleName());
        assertEquals("Guerrera", response.profession());
    }

    @Test
    void getCharacterById_deberiaLanzarNotFound_siHistoriaPrivadaYNoAutenticado() {
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);

        when(storyCharacterRepository.findById(5)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> characterService.getCharacterById(5)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getCharactersByStory_deberiaRetornarPaginaConFiltros() {
        Story story = mock(Story.class);
        StoryCharacter character = mock(StoryCharacter.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<StoryCharacter> page = new PageImpl<>(List.of(character), PageRequest.of(0, 20), 1);
        when(storyCharacterRepository.findByStoryWithFilters(eq(10), eq(true), eq("protagonist"), any(Pageable.class)))
                .thenReturn(page);

        when(character.getId()).thenReturn(1);
        when(character.getName()).thenReturn("Alicia");
        when(character.getCharacterRoleName()).thenReturn("protagonist");

        PageResponse<CharacterListItemResponse> response =
                characterService.getCharactersByStory(10, true, "protagonist", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Alicia", response.content().get(0).name());
        assertEquals("protagonist", response.content().get(0).characterRoleName());
    }

    @Test
    void searchCharacters_deberiaRetornarSoloPersonajesVisibles() {
        StoryCharacter visibleCharacter = mock(StoryCharacter.class);
        StoryCharacter hiddenCharacter = mock(StoryCharacter.class);
        Story publicStory = mock(Story.class);
        Story privateStory = mock(Story.class);

        Page<StoryCharacter> page = new PageImpl<>(
                List.of(visibleCharacter, hiddenCharacter),
                PageRequest.of(0, 20),
                2
        );

        when(storyCharacterRepository.searchByName(eq("ali"), any(Pageable.class))).thenReturn(page);

        when(visibleCharacter.getId()).thenReturn(1);
        when(visibleCharacter.getName()).thenReturn("Alicia");
        when(visibleCharacter.getStoryId()).thenReturn(10);

        when(hiddenCharacter.getId()).thenReturn(2);
        when(hiddenCharacter.getName()).thenReturn("Alina");
        when(hiddenCharacter.getStoryId()).thenReturn(20);

        when(storyRepository.findById(10)).thenReturn(Optional.of(publicStory));
        when(publicStory.getVisibilityState()).thenReturn("public");
        when(publicStory.getPublicationState()).thenReturn("published");
        when(publicStory.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(20)).thenReturn(Optional.of(privateStory));
        when(privateStory.getVisibilityState()).thenReturn("private");
        when(privateStory.getPublicationState()).thenReturn("draft");
        when(privateStory.getArchivedAt()).thenReturn(null);

        PageResponse<CharacterSearchItemResponse> response =
                characterService.searchCharacters("ali", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals(1, response.content().get(0).id());
        assertEquals("Alicia", response.content().get(0).name());
        assertEquals(10, response.content().get(0).storyId());

        assertEquals(2, response.totalElements());
    }

    @Test
    void updateCharacter_deberiaActualizarPersonaje() {
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        StoryCharacter saved = mock(StoryCharacter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyCharacterRepository.findById(5)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(storyCharacterRepository.save(character)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getName()).thenReturn("Alicia actualizada");
        when(saved.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        UpdateCharacterRequest request = new UpdateCharacterRequest(
                "Alicia actualizada",
                "Nueva descripción",
                "support",
                "Arquera",
                "Puntería",
                21,
                LocalDate.of(2004, 1, 1),
                true,
                List.of("support", "archer"),
                "https://img.test/new.jpg"
        );

        UpdateCharacterResponse response = characterService.updateCharacter(5, request);

        assertEquals(5, response.id());
        assertEquals("Alicia actualizada", response.name());
        assertEquals(LocalDateTime.of(2026, 4, 22, 12, 0), response.updatedAt());

        verify(character).setName("Alicia actualizada");
        verify(character).setDescription("Nueva descripción");
        verify(character).setCharacterRoleName("support");
        verify(character).setProfession("Arquera");
        verify(character).setAbility("Puntería");
        verify(character).setAge(21);
        verify(character).setBirthDate(LocalDate.of(2004, 1, 1));
        verify(character).setIsAlive(true);
        verify(character).setRolesJson("[\"support\",\"archer\"]");
        verify(character).setImageUrl("https://img.test/new.jpg");
    }

    @Test
    void deleteCharacter_deberiaEliminarPersonaje() {
        StoryCharacter character = mock(StoryCharacter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyCharacterRepository.findById(5)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        MessageResponse response = characterService.deleteCharacter(5);

        assertEquals("Personaje eliminado correctamente", response.message());
        verify(storyCharacterRepository).delete(character);
    }

    @Test
    void getAuthenticatedUser_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> characterService.createCharacter(new CreateCharacterRequest(
                        10, "Alicia", null, null, null, null,
                        null, null, null, null, null
                ))
        );

        assertEquals("No autenticado", ex.getMessage());
    }


    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}