package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AppClock;

import com.nunclear.escritores.dto.request.CreateStoryRequest;
import com.nunclear.escritores.dto.request.DuplicateStoryRequest;
import com.nunclear.escritores.dto.request.UpdateStoryRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private StoryService storyService;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStory_deberiaCrearHistoriaDraft() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);

        when(storyRepository.existsBySlugText("mi-historia")).thenReturn(false);
        when(storyRepository.save(any(Story.class))).thenReturn(saved);

        when(saved.getId()).thenReturn(10);
        when(saved.getOwnerUserId()).thenReturn(1);
        when(saved.getTitle()).thenReturn("Mi Historia");
        when(saved.getSlugText()).thenReturn("mi-historia");
        when(saved.getVisibilityState()).thenReturn("public");
        when(saved.getPublicationState()).thenReturn("draft");
        when(saved.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 10, 0));

        CreateStoryRequest request = new CreateStoryRequest(
                "Mi Historia",
                "Descripción",
                "https://img.test/cover.jpg",
                "public",
                "draft",
                true,
                true,
                LocalDate.of(2026, 1, 1)
        );

        CreateStoryResponse response = storyService.createStory(request);

        assertNotNull(response);
        assertEquals(10, response.id());
        assertEquals(1, response.ownerUserId());
        assertEquals("Mi Historia", response.title());
        assertEquals("mi-historia", response.slugText());
        assertEquals("public", response.visibilityState());
        assertEquals("draft", response.publicationState());

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(captor.capture());

        Story storyToSave = captor.getValue();
        assertEquals(1, storyToSave.getOwnerUserId());
        assertEquals("Mi Historia", storyToSave.getTitle());
        assertEquals("mi-historia", storyToSave.getSlugText());
        assertEquals("public", storyToSave.getVisibilityState());
        assertEquals("draft", storyToSave.getPublicationState());
        assertNull(storyToSave.getPublishedAt());
    }

    @Test
    void createStory_deberiaAsignarPublishedAt_siPublicationStateEsPublished() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);

        when(storyRepository.existsBySlugText("historia-publicada")).thenReturn(false);
        when(storyRepository.save(any(Story.class))).thenReturn(saved);

        when(saved.getId()).thenReturn(11);
        when(saved.getOwnerUserId()).thenReturn(1);
        when(saved.getTitle()).thenReturn("Historia Publicada");
        when(saved.getSlugText()).thenReturn("historia-publicada");
        when(saved.getVisibilityState()).thenReturn("public");
        when(saved.getPublicationState()).thenReturn("published");
        when(saved.getCreatedAt()).thenReturn(AppClock.now());

        CreateStoryRequest request = new CreateStoryRequest(
                "Historia Publicada",
                "Desc",
                null,
                "public",
                "published",
                true,
                true,
                null
        );

        storyService.createStory(request);

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(captor.capture());

        assertNotNull(captor.getValue().getPublishedAt());
    }

    @Test
    void createStory_deberiaLanzarBadRequest_siVisibilityStateEsInvalido() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        CreateStoryRequest request = new CreateStoryRequest(
                "Título",
                "Desc",
                null,
                "friends",
                "draft",
                true,
                true,
                null
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> storyService.createStory(request)
        );

        assertEquals("visibilityState inválido", ex.getMessage());
        verify(storyRepository, never()).save(any());
    }

    @Test
    void createStory_deberiaLanzarBadRequest_siPublicationStateEsInvalido() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        CreateStoryRequest request = new CreateStoryRequest(
                "Título",
                "Desc",
                null,
                "public",
                "scheduled",
                true,
                true,
                null
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> storyService.createStory(request)
        );

        assertEquals("publicationState inválido", ex.getMessage());
    }

    @Test
    void createStory_deberiaGenerarSlugUnico() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);

        when(storyRepository.existsBySlugText("mi-historia")).thenReturn(true);
        when(storyRepository.existsBySlugText("mi-historia-2")).thenReturn(false);
        when(storyRepository.save(any(Story.class))).thenReturn(saved);

        when(saved.getId()).thenReturn(1);
        when(saved.getOwnerUserId()).thenReturn(1);
        when(saved.getTitle()).thenReturn("Mi Historia");
        when(saved.getSlugText()).thenReturn("mi-historia-2");
        when(saved.getVisibilityState()).thenReturn("public");
        when(saved.getPublicationState()).thenReturn("draft");
        when(saved.getCreatedAt()).thenReturn(AppClock.now());

        CreateStoryRequest request = new CreateStoryRequest(
                "Mi Historia",
                null,
                null,
                "public",
                "draft",
                true,
                true,
                null
        );

        storyService.createStory(request);

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(captor.capture());

        assertEquals("mi-historia-2", captor.getValue().getSlugText());
    }

    @Test
    void createStory_deberiaLanzarBadRequest_siNoSePuedeGenerarSlug() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        CreateStoryRequest request = new CreateStoryRequest(
                "!!!",
                null,
                null,
                "public",
                "draft",
                true,
                true,
                null
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> storyService.createStory(request)
        );

        assertEquals("No se pudo generar slug para el título", ex.getMessage());
    }

    @Test
    void getStoryById_deberiaRetornarHistoriaPublicaPublicada() {
        Story story = mock(Story.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);
        when(story.getTitle()).thenReturn("Historia");
        when(story.getSlugText()).thenReturn("historia");
        when(story.getDescription()).thenReturn("Desc");
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        StoryDetailResponse response = storyService.getStoryById(10);

        assertEquals(10, response.id());
        assertEquals("Historia", response.title());
        assertEquals("historia", response.slugText());
    }

    @Test
    void getStoryById_deberiaLanzarNotFound_siHistoriaPrivadaYNoAutenticado() {
        Story story = mock(Story.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoryById(10)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getStoryBySlug_deberiaRetornarHistoria() {
        Story story = mock(Story.class);

        when(storyRepository.findBySlugText("slug-test")).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getSlugText()).thenReturn("slug-test");
        when(story.getTitle()).thenReturn("Historia");
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        StorySlugResponse response = storyService.getStoryBySlug("slug-test");

        assertEquals(10, response.id());
        assertEquals("slug-test", response.slugText());
        assertEquals("Historia", response.title());
    }

    @Test
    void listPublicStories_deberiaRetornarPageResponse() {
        Story story = mock(Story.class);
        when(story.getId()).thenReturn(1);
        when(story.getTitle()).thenReturn("Historia 1");
        when(story.getSlugText()).thenReturn("historia-1");
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");

        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);

        when(storyRepository.findByVisibilityStateIgnoreCaseAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                eq("public"), eq("published"), any(Pageable.class))
        ).thenReturn(page);

        PageResponse<StoryListItemResponse> response = storyService.listPublicStories(0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Historia 1", response.content().get(0).title());
        assertEquals(1, response.totalElements());
    }

    @Test
    void searchStories_deberiaBuscarSoloPublicPublished() {
        Story story = mock(Story.class);
        when(story.getId()).thenReturn(1);
        when(story.getTitle()).thenReturn("Historia");
        when(story.getSlugText()).thenReturn("historia");
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");

        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);

        when(storyRepository.searchPublicStories(eq("aventura"), eq("public"), eq("published"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<StoryListItemResponse> response =
                storyService.searchStories("aventura", "public", "published", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Historia", response.content().get(0).title());
    }

    @Test
    void searchStories_deberiaLanzarBadRequest_siBuscaAlgoNoPublico() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> storyService.searchStories("aventura", "private", "published", 0, 20, null)
        );

        assertEquals("Solo se permite búsqueda pública de historias publicadas", ex.getMessage());
    }

    @Test
    void getStoriesByUser_deberiaRetornarSoloPublicadas_siNoPuedeVerPrivadas() {
        Story story = mock(Story.class);
        when(story.getId()).thenReturn(1);
        when(story.getOwnerUserId()).thenReturn(7);
        when(story.getTitle()).thenReturn("Historia");
        when(story.getPublicationState()).thenReturn("published");

        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);

        when(storyRepository.findPublicPublishedByOwner(eq(7), any(Pageable.class))).thenReturn(page);

        PageResponse<UserStorySummaryResponse> response =
                storyService.getStoriesByUser(7, true, 0, 20, null);

        assertEquals(1, response.content().size());
        verify(storyRepository).findPublicPublishedByOwner(eq(7), any(Pageable.class));
        verify(storyRepository, never()).findAllVisibleForOwner(anyInt(), any(Pageable.class));
    }

    @Test
    void getStoriesByUser_deberiaRetornarTodas_siEsOwnerEIncludeDraftsTrue() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story story = mock(Story.class);

        when(principal.getId()).thenReturn(7);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(7)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7);

        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);
        when(storyRepository.findAllVisibleForOwner(eq(7), any(Pageable.class))).thenReturn(page);

        when(story.getId()).thenReturn(1);
        when(story.getOwnerUserId()).thenReturn(7);
        when(story.getTitle()).thenReturn("Mi historia");
        when(story.getPublicationState()).thenReturn("draft");

        PageResponse<UserStorySummaryResponse> response =
                storyService.getStoriesByUser(7, true, 0, 20, null);

        assertEquals(1, response.content().size());
        verify(storyRepository).findAllVisibleForOwner(eq(7), any(Pageable.class));
    }

    @Test
    void getMyDrafts_deberiaRetornarDraftsDelUsuarioActual() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story story = mock(Story.class);

        when(principal.getId()).thenReturn(5);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(5)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(5);

        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);

        when(storyRepository.findByOwnerUserIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                eq(5), eq("draft"), any(Pageable.class))
        ).thenReturn(page);

        when(story.getId()).thenReturn(1);
        when(story.getOwnerUserId()).thenReturn(5);
        when(story.getTitle()).thenReturn("Borrador");
        when(story.getPublicationState()).thenReturn("draft");

        PageResponse<UserStorySummaryResponse> response = storyService.getMyDrafts(0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Borrador", response.content().get(0).title());
    }

    @Test
    void getMyArchived_deberiaRetornarArchivadas() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story story = mock(Story.class);

        when(principal.getId()).thenReturn(5);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(5)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(5);

        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);

        when(storyRepository.findByOwnerUserIdAndArchivedAtIsNotNull(eq(5), any(Pageable.class)))
                .thenReturn(page);

        when(story.getId()).thenReturn(1);
        when(story.getTitle()).thenReturn("Archivada");
        when(story.getArchivedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        PageResponse<ArchivedStoryItemResponse> response = storyService.getMyArchived(0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Archivada", response.content().get(0).title());
    }

    @Test
    void updateStory_deberiaActualizarHistoria_siEsOwner() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);
        when(story.getId()).thenReturn(10);

        when(storyRepository.findBySlugText("nuevo-titulo")).thenReturn(Optional.empty());
        when(storyRepository.save(story)).thenReturn(saved);

        when(saved.getId()).thenReturn(10);
        when(saved.getTitle()).thenReturn("Nuevo Título");
        when(saved.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 13, 0));

        UpdateStoryRequest request = new UpdateStoryRequest(
                "Nuevo Título",
                "Nueva desc",
                "https://img.com/nueva.jpg",
                "private",
                false,
                false
        );

        UpdateStoryResponse response = storyService.updateStory(10, request);

        assertEquals(10, response.id());
        assertEquals("Nuevo Título", response.title());

        verify(story).setTitle("Nuevo Título");
        verify(story).setSlugText("nuevo-titulo");
        verify(story).setDescription("Nueva desc");
        verify(story).setCoverImageUrl("https://img.com/nueva.jpg");
        verify(story).setVisibilityState("private");
        verify(story).setAllowFeedback(false);
        verify(story).setAllowScores(false);
    }

    @Test
    void updateStory_deberiaLanzarUnauthorized_siNoEsOwnerNiAdminNiModerator() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(99);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(99)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(99);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        UpdateStoryRequest request = new UpdateStoryRequest(
                "Nuevo",
                "Desc",
                null,
                "public",
                true,
                true
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.updateStory(10, request)
        );

        assertEquals("No tienes permisos para modificar esta historia", ex.getMessage());
    }

    @Test
    void publishStory_deberiaPublicarHistoria() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(storyRepository.save(story)).thenReturn(saved);
        when(saved.getId()).thenReturn(10);
        when(saved.getPublicationState()).thenReturn("published");
        when(saved.getPublishedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 14, 0));

        StoryPublicationResponse response = storyService.publishStory(10);

        assertEquals(10, response.id());
        assertEquals("published", response.publicationState());
        verify(story).setPublicationState("published");
        verify(story).setPublishedAt(any(LocalDateTime.class));
    }

    @Test
    void unpublishStory_deberiaPasarADraft() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(storyRepository.save(story)).thenReturn(saved);
        when(saved.getId()).thenReturn(10);
        when(saved.getPublicationState()).thenReturn("draft");
        when(saved.getPublishedAt()).thenReturn(null);

        StoryPublicationResponse response = storyService.unpublishStory(10);

        assertEquals("draft", response.publicationState());
        assertNull(response.publishedAt());
        verify(story).setPublicationState("draft");
        verify(story).setPublishedAt(null);
    }

    @Test
    void archiveStory_deberiaAsignarArchivedAt() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(storyRepository.save(story)).thenReturn(saved);
        when(saved.getId()).thenReturn(10);
        when(saved.getArchivedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 15, 0));

        StoryArchiveResponse response = storyService.archiveStory(10);

        assertEquals(10, response.id());
        assertNotNull(response.archivedAt());
        verify(story).setArchivedAt(any(LocalDateTime.class));
    }

    @Test
    void restoreStory_deberiaDejarArchivedAtEnNull() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(storyRepository.save(story)).thenReturn(saved);
        when(saved.getId()).thenReturn(10);
        when(saved.getArchivedAt()).thenReturn(null);

        StoryArchiveResponse response = storyService.restoreStory(10);

        assertEquals(10, response.id());
        assertNull(response.archivedAt());
        verify(story).setArchivedAt(null);
    }

    @Test
    void duplicateStory_deberiaDuplicarHistoriaComoDraft() {
        Story source = mock(Story.class);
        AppUser currentUser = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story saved = mock(Story.class);

        when(principal.getId()).thenReturn(2);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(currentUser));
        when(currentUser.getId()).thenReturn(2);

        when(storyRepository.findById(10)).thenReturn(Optional.of(source));
        when(source.getVisibilityState()).thenReturn("public");
        when(source.getPublicationState()).thenReturn("published");
        when(source.getArchivedAt()).thenReturn(null);
        when(source.getDescription()).thenReturn("Desc original");
        when(source.getCoverImageUrl()).thenReturn("https://img.com/old.jpg");
        when(source.getAllowFeedback()).thenReturn(true);
        when(source.getAllowScores()).thenReturn(false);
        when(source.getStartedOn()).thenReturn(LocalDate.of(2025, 1, 1));
        when(source.getId()).thenReturn(10);

        when(storyRepository.existsBySlugText("copia-de-historia")).thenReturn(false);
        when(storyRepository.save(any(Story.class))).thenReturn(saved);

        when(saved.getId()).thenReturn(20);
        when(saved.getTitle()).thenReturn("Copia de Historia");
        when(saved.getPublicationState()).thenReturn("draft");

        DuplicateStoryResponse response = storyService.duplicateStory(
                10,
                new DuplicateStoryRequest("Copia de Historia")
        );

        assertEquals(20, response.id());
        assertEquals(10, response.sourceStoryId());
        assertEquals("Copia de Historia", response.title());
        assertEquals("draft", response.publicationState());

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(captor.capture());

        Story duplicate = captor.getValue();
        assertEquals(2, duplicate.getOwnerUserId());
        assertEquals("Copia de Historia", duplicate.getTitle());
        assertEquals("draft", duplicate.getPublicationState());
        assertNull(duplicate.getPublishedAt());
        assertNull(duplicate.getArchivedAt());
    }

    @Test
    void deleteStory_deberiaEliminarHistoria() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        MessageResponse response = storyService.deleteStory(10);

        assertEquals("Historia eliminada correctamente", response.message());
        verify(storyRepository).delete(story);
    }

    @Test
    void getAuthenticatedUser_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.getMyDrafts(0, 20, null)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    // ==================== ADDITIONAL COVERAGE TESTS ====================

    @Test
    void createStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        CreateStoryRequest request = new CreateStoryRequest(
                "Título",
                "Desc",
                null,
                "public",
                "draft",
                true,
                true,
                null
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.createStory(request)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void createStory_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        CreateStoryRequest request = new CreateStoryRequest(
                "Título",
                "Desc",
                null,
                "public",
                "draft",
                true,
                true,
                null
        );

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.createStory(request)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getStoryById_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoryById(999)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getStoryById_deberiaLanzarNotFound_siHistoriaArchivada() {
        Story story = mock(Story.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getArchivedAt()).thenReturn(AppClock.now());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoryById(10)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getStoryBySlug_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        when(storyRepository.findBySlugText("no-existe")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoryBySlug("no-existe")
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getStoryBySlug_deberiaLanzarNotFound_siHistoriaPrivada() {
        Story story = mock(Story.class);

        when(storyRepository.findBySlugText("privada")).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoryBySlug("privada")
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void listPublicStories_deberiaRetornarListaVacia() {
        Page<Story> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(storyRepository.findByVisibilityStateIgnoreCaseAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                eq("public"), eq("published"), any(Pageable.class))
        ).thenReturn(page);

        PageResponse<StoryListItemResponse> response = storyService.listPublicStories(0, 20, null);

        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
    }

    @Test
    void listPublicStories_deberiaRetornarMultiplesStories() {
        Story story1 = mock(Story.class);
        Story story2 = mock(Story.class);
        Page<Story> page = new PageImpl<>(List.of(story1, story2), PageRequest.of(0, 20), 2);

        when(storyRepository.findByVisibilityStateIgnoreCaseAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                eq("public"), eq("published"), any(Pageable.class))
        ).thenReturn(page);

        when(story1.getId()).thenReturn(1);
        when(story1.getTitle()).thenReturn("Historia 1");
        when(story1.getSlugText()).thenReturn("historia-1");
        when(story1.getVisibilityState()).thenReturn("public");
        when(story1.getPublicationState()).thenReturn("published");

        when(story2.getId()).thenReturn(2);
        when(story2.getTitle()).thenReturn("Historia 2");
        when(story2.getSlugText()).thenReturn("historia-2");
        when(story2.getVisibilityState()).thenReturn("public");
        when(story2.getPublicationState()).thenReturn("published");

        PageResponse<StoryListItemResponse> response = storyService.listPublicStories(0, 20, "createdAt,desc");

        assertEquals(2, response.content().size());
        assertEquals("Historia 1", response.content().get(0).title());
        assertEquals("Historia 2", response.content().get(1).title());
    }

    @Test
    void searchStories_deberiaLanzarBadRequest_siBuscaDraft() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> storyService.searchStories("aventura", "public", "draft", 0, 20, null)
        );

        assertEquals("Solo se permite búsqueda pública de historias publicadas", ex.getMessage());
    }

    @Test
    void searchStories_deberiaLanzarBadRequest_siBuscaPrivate() {
        String visibilityState = "private";

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> storyService.searchStories("misterio", visibilityState, "published", 0, 20, "title,asc")
        );

        assertEquals("Solo se permite búsqueda pública de historias publicadas", ex.getMessage());
        verify(storyRepository, never()).searchPublicStories(anyString(), anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void searchStories_deberiaRetornarVacio() {
        Page<Story> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(storyRepository.searchPublicStories(eq("inexistente"), eq("public"), eq("published"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<StoryListItemResponse> response =
                storyService.searchStories("inexistente", "public", "published", 0, 20, null);

        assertEquals(0, response.content().size());
    }

    @Test
    void getStoriesByUser_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoriesByUser(999, true, 0, 20, null)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getMyDrafts_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.clearContext();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.getMyDrafts(0, 20, "createdAt,desc")
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void getMyDrafts_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getMyDrafts(0, 20, null)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getMyDrafts_deberiaRetornarVacio() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Page<Story> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(principal.getId()).thenReturn(5);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(5)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(5);

        when(storyRepository.findByOwnerUserIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                eq(5), eq("draft"), any(Pageable.class))
        ).thenReturn(page);

        PageResponse<UserStorySummaryResponse> response = storyService.getMyDrafts(0, 20, null);

        assertEquals(0, response.content().size());
    }

    @Test
    void getMyArchived_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.getMyArchived(0, 20, null)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void getMyArchived_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getMyArchived(0, 20, null)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getMyArchived_deberiaRetornarVacio() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Page<Story> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(principal.getId()).thenReturn(5);
        mockAuthenticatedUser(principal);

        when(appUserRepository.findById(5)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(5);

        when(storyRepository.findByOwnerUserIdAndArchivedAtIsNotNull(eq(5), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ArchivedStoryItemResponse> response = storyService.getMyArchived(0, 20, null);

        assertEquals(0, response.content().size());
    }

    @Test
    void updateStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UpdateStoryRequest request = new UpdateStoryRequest(
                "Nuevo",
                "Desc",
                null,
                "public",
                true,
                true
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.updateStory(10, request)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void updateStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        UpdateStoryRequest request = new UpdateStoryRequest(
                "Nuevo",
                "Desc",
                null,
                "public",
                true,
                true
        );

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.updateStory(999, request)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void publishStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.publishStory(10)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void publishStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.publishStory(999)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void unpublishStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.unpublishStory(10)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void unpublishStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.unpublishStory(999)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void archiveStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.archiveStory(10)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void archiveStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.archiveStory(999)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void restoreStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.restoreStory(10)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void restoreStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.restoreStory(999)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void duplicateStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        DuplicateStoryRequest request = new DuplicateStoryRequest("Copia");

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.duplicateStory(10, request)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void duplicateStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(2);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(2)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        DuplicateStoryRequest request = new DuplicateStoryRequest("Copia");

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.duplicateStory(999, request)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void deleteStory_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.deleteStory(10)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void deleteStory_deberiaLanzarNotFound_siHistoriaNoEncontrada() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(mock(AppUser.class)));

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.deleteStory(999)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void deleteStory_deberiaLanzarUnauthorized_siNoEsOwnerNiAdmin() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(99);
        mockAuthenticatedUser(principal);
        when(appUserRepository.findById(99)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(99);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> storyService.deleteStory(10)
        );

        assertEquals("No tienes permisos para eliminar esta historia", ex.getMessage());
    }

    private void mockAuthenticatedUser(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}
