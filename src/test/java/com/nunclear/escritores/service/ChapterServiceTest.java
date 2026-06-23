package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Chapter;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.Volume;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ChapterRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.repository.VolumeRepository;
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
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private VolumeRepository volumeRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ChapterService chapterService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createChapter_deberiaCrearCapituloYCalcularMetricas() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Chapter saved = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(any(Chapter.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getTitle()).thenReturn("Capítulo 1");
        when(saved.getPublicationState()).thenReturn("draft");
        when(saved.getReadingMinutes()).thenReturn(1);
        when(saved.getWordCount()).thenReturn(5);

        CreateChapterRequest request = new CreateChapterRequest(
                10,
                null,
                "Capítulo 1",
                "Sub",
                "uno dos tres cuatro cinco",
                null,
                "draft",
                1
        );

        CreateChapterResponse response = chapterService.createChapter(request);

        assertNotNull(response);
        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Capítulo 1", response.title());
        assertEquals("draft", response.publicationState());
        assertEquals(5, response.wordCount());
        assertEquals(1, response.readingMinutes());

        ArgumentCaptor<Chapter> captor = ArgumentCaptor.forClass(Chapter.class);
        verify(chapterRepository).save(captor.capture());

        Chapter toSave = captor.getValue();
        assertEquals(10, toSave.getStoryId());
        assertEquals("Capítulo 1", toSave.getTitle());
        assertEquals("draft", toSave.getPublicationState());
        assertEquals(1, toSave.getPositionIndex());
        assertEquals(5, toSave.getWordCount());
        assertEquals(1, toSave.getReadingMinutes());
    }

    @Test
    void createChapter_deberiaLanzarBadRequest_siPublicationStateInvalido() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        CreateChapterRequest request = new CreateChapterRequest(
                10,
                null,
                "Capítulo",
                null,
                "texto",
                null,
                "scheduled",
                1
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> chapterService.createChapter(request)
        );

        assertEquals("publicationState inválido", ex.getMessage());
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void createChapter_deberiaLanzarBadRequest_siVolumeNoPerteneceALaHistoria() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.findByIdAndStoryId(5, 10)).thenReturn(Optional.empty());

        CreateChapterRequest request = new CreateChapterRequest(
                10,
                5,
                "Capítulo",
                null,
                "texto",
                null,
                "draft",
                1
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> chapterService.createChapter(request)
        );

        assertEquals("El volumen no pertenece a la historia", ex.getMessage());
    }

    @Test
    void getChapterById_deberiaRetornarCapituloPublico() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);

        when(chapterRepository.findById(11)).thenReturn(Optional.of(chapter));
        when(chapter.getId()).thenReturn(11);
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getTitle()).thenReturn("Capítulo público");
        when(chapter.getContent()).thenReturn("contenido");
        when(chapter.getPublicationState()).thenReturn("published");
        when(chapter.getWordCount()).thenReturn(50);
        when(chapter.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        ChapterDetailResponse response = chapterService.getChapterById(11);

        assertEquals(11, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Capítulo público", response.title());
        assertEquals("published", response.publicationState());
        assertEquals(50, response.wordCount());
    }

    @Test
    void getChapterById_deberiaLanzarNotFound_siNoEsPublicoYNoAutenticado() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);

        when(chapterRepository.findById(11)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getPublicationState()).thenReturn("draft");
        when(chapter.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> chapterService.getChapterById(11)
        );

        assertEquals("Capítulo no encontrado", ex.getMessage());
    }

    @Test
    void getChaptersByStory_deberiaRetornarActivos_siIncludeDraftsYPuedeVerlos() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story story = mock(Story.class);
        Chapter chapter = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        Page<Chapter> page = new PageImpl<>(List.of(chapter), PageRequest.of(0, 20), 1);

        when(chapterRepository.findPageActiveByStoryId(eq(10), any(Pageable.class))).thenReturn(page);

        when(chapter.getId()).thenReturn(7);
        when(chapter.getTitle()).thenReturn("Cap 1");
        when(chapter.getPositionIndex()).thenReturn(1);
        when(chapter.getPublicationState()).thenReturn("draft");
        when(chapter.getArchivedAt()).thenReturn(null);

        PageResponse<ChapterListItemResponse> response =
                chapterService.getChaptersByStory(10, true, 0, 20, null);

        assertEquals(1, response.content().size());
        verify(chapterRepository).findPageActiveByStoryId(eq(10), any(Pageable.class));
        verify(chapterRepository, never()).findPagePublishedByStoryId(anyInt(), any(Pageable.class));
    }

    @Test
    void getChaptersByStory_deberiaRetornarPublished_siNoPuedeVerDrafts() {
        Chapter chapter = mock(Chapter.class);
        Page<Chapter> page = new PageImpl<>(List.of(chapter), PageRequest.of(0, 20), 1);

        when(chapterRepository.findPagePublishedByStoryId(eq(10), any(Pageable.class))).thenReturn(page);

        when(chapter.getId()).thenReturn(7);
        when(chapter.getTitle()).thenReturn("Cap 1");
        when(chapter.getPositionIndex()).thenReturn(1);
        when(chapter.getPublicationState()).thenReturn("published");
        when(chapter.getArchivedAt()).thenReturn(null);

        PageResponse<ChapterListItemResponse> response =
                chapterService.getChaptersByStory(10, true, 0, 20, null);

        assertEquals(1, response.content().size());
        verify(chapterRepository).findPagePublishedByStoryId(eq(10), any(Pageable.class));
    }

    @Test
    void getPublishedChaptersByStory_deberiaRetornarSoloPublicados() {
        Chapter chapter = mock(Chapter.class);
        Page<Chapter> page = new PageImpl<>(List.of(chapter), PageRequest.of(0, 20), 1);

        when(chapterRepository.findPagePublishedByStoryId(eq(10), any(Pageable.class))).thenReturn(page);

        when(chapter.getId()).thenReturn(8);
        when(chapter.getTitle()).thenReturn("Publicado");
        when(chapter.getPositionIndex()).thenReturn(2);
        when(chapter.getPublicationState()).thenReturn("published");
        when(chapter.getArchivedAt()).thenReturn(null);

        PageResponse<ChapterListItemResponse> response =
                chapterService.getPublishedChaptersByStory(10, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Publicado", response.content().get(0).title());
    }

    @Test
    void getMyDrafts_deberiaRetornarVacio_siUsuarioNoTieneHistorias() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);

        when(storyRepository.findAll()).thenReturn(List.of());

        PageResponse<ChapterListItemResponse> response = chapterService.getMyDrafts(null, 0, 20, null);

        assertNotNull(response);
        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
    }

    @Test
    void getMyDrafts_deberiaBuscarPorStoryIds_siStoryIdEsNull() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story story = mock(Story.class);
        Chapter chapter = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);

        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);
        when(storyRepository.findAll()).thenReturn(List.of(story));

        Page<Chapter> page = new PageImpl<>(List.of(chapter), PageRequest.of(0, 20), 1);
        when(chapterRepository.findDraftsByStoryIds(eq(List.of(10)), any(Pageable.class))).thenReturn(page);

        when(chapter.getId()).thenReturn(100);
        when(chapter.getTitle()).thenReturn("Draft");
        when(chapter.getPositionIndex()).thenReturn(1);
        when(chapter.getPublicationState()).thenReturn("draft");
        when(chapter.getArchivedAt()).thenReturn(null);

        PageResponse<ChapterListItemResponse> response = chapterService.getMyDrafts(null, 0, 20, null);

        assertEquals(1, response.content().size());
        verify(chapterRepository).findDraftsByStoryIds(eq(List.of(10)), any(Pageable.class));
    }

    @Test
    void getMyDrafts_deberiaBuscarDraftsDeUnaHistoria_siStoryIdVieneInformado() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Story story = mock(Story.class);
        Chapter chapter = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        Page<Chapter> page = new PageImpl<>(List.of(chapter), PageRequest.of(0, 20), 1);
        when(chapterRepository.findDraftsByStoryId(eq(10), any(Pageable.class))).thenReturn(page);

        when(chapter.getId()).thenReturn(100);
        when(chapter.getTitle()).thenReturn("Draft");
        when(chapter.getPositionIndex()).thenReturn(1);
        when(chapter.getPublicationState()).thenReturn("draft");
        when(chapter.getArchivedAt()).thenReturn(null);

        PageResponse<ChapterListItemResponse> response = chapterService.getMyDrafts(10, 0, 20, null);

        assertEquals(1, response.content().size());
        verify(chapterRepository).findDraftsByStoryId(eq(10), any(Pageable.class));
    }

    @Test
    void searchChapters_deberiaRetornarResultadosConExcerpt() {
        Chapter chapter = mock(Chapter.class);
        Page<Chapter> page = new PageImpl<>(List.of(chapter), PageRequest.of(0, 20), 1);

        when(chapterRepository.searchPublishedChapters(eq("aventura"), eq(10), any(Pageable.class)))
                .thenReturn(page);

        when(chapter.getId()).thenReturn(3);
        when(chapter.getTitle()).thenReturn("Capítulo aventura");
        when(chapter.getContent()).thenReturn("Este es un contenido de prueba para construir un excerpt del capítulo.");

        PageResponse<ChapterSearchItemResponse> response =
                chapterService.searchChapters("aventura", 10, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals(3, response.content().get(0).id());
        assertEquals("Capítulo aventura", response.content().get(0).title());
        assertTrue(response.content().get(0).excerpt().contains("contenido de prueba"));
    }

    @Test
    void updateChapter_deberiaActualizarCapituloYRecalcularMetricas() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Chapter saved = mock(Chapter.class);
        Volume volume = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.findByIdAndStoryId(20, 10)).thenReturn(Optional.of(volume));
        when(chapterRepository.save(chapter)).thenReturn(saved);

        when(saved.getId()).thenReturn(5);
        when(saved.getTitle()).thenReturn("Nuevo");
        when(saved.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, Month.APRIL, 22, 12, 0));

        UpdateChapterRequest request = new UpdateChapterRequest(
                "Nuevo",
                "Sub nuevo",
                "uno dos tres cuatro",
                20,
                2
        );

        UpdateChapterResponse response = chapterService.updateChapter(5, request);

        assertEquals(5, response.id());
        assertEquals("Nuevo", response.title());

        verify(chapter).setTitle("Nuevo");
        verify(chapter).setSubtitle("Sub nuevo");
        verify(chapter).setContent("uno dos tres cuatro");
        verify(chapter).setVolumeId(20);
        verify(chapter).setPositionIndex(2);
        verify(chapter).setWordCount(4);
        verify(chapter).setReadingMinutes(1);
    }

    @Test
    void publishChapter_deberiaPublicarYAsignarFecha_siEsNull() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Chapter saved = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getPublishedOn()).thenReturn(null);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getPublicationState()).thenReturn("published");

        ChapterPublicationStateResponse response = chapterService.publishChapter(5);

        assertEquals(5, response.id());
        assertEquals("published", response.publicationState());
        verify(chapter).setPublicationState("published");
        verify(chapter).setPublishedOn(any(LocalDate.class));
    }

    @Test
    void unpublishChapter_deberiaPasarADraft() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Chapter saved = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getPublicationState()).thenReturn("draft");

        ChapterPublicationStateResponse response = chapterService.unpublishChapter(5);

        assertEquals("draft", response.publicationState());
        verify(chapter).setPublicationState("draft");
    }

    @Test
    void archiveChapter_deberiaAsignarArchivedAt() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Chapter saved = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getArchivedAt()).thenReturn(LocalDateTime.of(2026, Month.APRIL, 22, 13, 0));

        ChapterArchiveResponse response = chapterService.archiveChapter(5);

        assertEquals(5, response.id());
        assertNotNull(response.archivedAt());
        verify(chapter).setArchivedAt(any(LocalDateTime.class));
    }

    @Test
    void reorderChapters_deberiaActualizarPosiciones() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        Chapter ch1 = mock(Chapter.class);
        Chapter ch2 = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(ch1.getId()).thenReturn(100);
        when(ch1.getStoryId()).thenReturn(10);
        when(ch1.getArchivedAt()).thenReturn(null);

        when(ch2.getId()).thenReturn(101);
        when(ch2.getStoryId()).thenReturn(10);
        when(ch2.getArchivedAt()).thenReturn(null);

        when(chapterRepository.findAllById(any())).thenReturn(List.of(ch1, ch2));

        ReorderChaptersRequest request = new ReorderChaptersRequest(
                10,
                List.of(
                        new ReorderChapterItemRequest(100, 2),
                        new ReorderChapterItemRequest(101, 1)
                )
        );

        MessageResponse response = chapterService.reorderChapters(request);

        assertEquals("Capítulos reordenados correctamente", response.message());
        verify(ch1).setPositionIndex(2);
        verify(ch2).setPositionIndex(1);
        verify(chapterRepository).saveAll(anyList());
    }

    @Test
    void reorderChapters_deberiaLanzarBadRequest_siFaltanCapitulos() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.findAllById(any())).thenReturn(List.of());

        ReorderChaptersRequest request = new ReorderChaptersRequest(
                10,
                List.of(new ReorderChapterItemRequest(100, 1))
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> chapterService.reorderChapters(request)
        );

        assertEquals("Uno o más capítulos no existen", ex.getMessage());
    }

    @Test
    void moveChapter_deberiaMoverAVolumenDestino() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume volume = mock(Volume.class);
        Chapter saved = mock(Chapter.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(9)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.findByIdAndStoryId(7, 10)).thenReturn(Optional.of(volume));
        when(volume.getId()).thenReturn(7);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(9);
        when(saved.getVolumeId()).thenReturn(7);
        when(saved.getPositionIndex()).thenReturn(4);

        MoveChapterRequest request = new MoveChapterRequest(7, 4);

        MoveChapterResponse response = chapterService.moveChapter(9, request);

        assertEquals(9, response.id());
        assertEquals(7, response.volumeId());
        assertEquals(4, response.positionIndex());

        verify(chapter).setVolumeId(7);
        verify(chapter).setPositionIndex(4);
    }

    @Test
    void moveChapter_deberiaLanzarBadRequest_siVolumenDestinoNoExiste() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(9)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.findByIdAndStoryId(7, 10)).thenReturn(Optional.empty());

        MoveChapterRequest request = new MoveChapterRequest(7, 4);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> chapterService.moveChapter(9, request)
        );

        assertEquals("El volumen destino no existe o no pertenece a la historia", ex.getMessage());
        verify(chapterRepository, never()).save(any(Chapter.class));
    }

    @Test
    void deleteChapter_deberiaEliminarCapitulo() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(9)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        MessageResponse response = chapterService.deleteChapter(9);

        assertEquals("Capítulo eliminado correctamente", response.message());
        verify(chapterRepository).delete(chapter);
    }

    @Test
    void getEditableStory_deberiaLanzarUnauthorized_siNoEsOwnerNiModeratorNiAdmin() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(99);
        mockAuthenticated(principal);

        when(appUserRepository.findById(99)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(99);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        CreateChapterRequest request = new CreateChapterRequest(
                10,
                null,
                "Capítulo",
                null,
                "texto",
                null,
                "draft",
                1
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> chapterService.createChapter(request)
        );

        assertEquals("No tienes permisos sobre esta historia", ex.getMessage());
    }

    @Test
    void getAuthenticatedUser_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> chapterService.getMyDrafts(null, 0, 20, null)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void moveChapter_deberiaMoverCapituloAVolumenDestinoYGuardarCambios() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        Volume targetVolume = mock(Volume.class);
        AppUser user = mock(AppUser.class);
        Chapter saved = mock(Chapter.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(9)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.findByIdAndStoryId(7, 10)).thenReturn(Optional.of(targetVolume));
        when(targetVolume.getId()).thenReturn(7);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(9);
        when(saved.getVolumeId()).thenReturn(7);
        when(saved.getPositionIndex()).thenReturn(4);

        MoveChapterRequest request = new MoveChapterRequest(7, 4);

        MoveChapterResponse response = chapterService.moveChapter(9, request);

        assertEquals(9, response.id());
        assertEquals(7, response.volumeId());
        assertEquals(4, response.positionIndex());

        verify(chapter).setVolumeId(7);
        verify(chapter).setPositionIndex(4);
        verify(chapterRepository).save(chapter);
    }

    @Test
    void moveChapter_deberiaLanzarBadRequest_siVolumenDestinoNoPerteneceALaHistoria() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(9)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.findByIdAndStoryId(99, 10)).thenReturn(Optional.empty());

        MoveChapterRequest request = new MoveChapterRequest(99, 1);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> chapterService.moveChapter(9, request)
        );

        assertEquals("El volumen destino no existe o no pertenece a la historia", ex.getMessage());
        verify(volumeRepository).findByIdAndStoryId(99, 10);
        verify(chapter, never()).setVolumeId(anyInt());
        verify(chapter, never()).setPositionIndex(anyInt());
        verify(chapterRepository, never()).save(any(Chapter.class));
    }

    @Test
    void publishChapter_deberiaCambiarEstadoAPublishedYAsignarFechaSiEsNull() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Chapter saved = mock(Chapter.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getPublishedOn()).thenReturn(null);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getPublicationState()).thenReturn("published");

        ChapterPublicationStateResponse response = chapterService.publishChapter(5);

        assertEquals(5, response.id());
        assertEquals("published", response.publicationState());

        verify(chapter).setPublicationState("published");
        verify(chapter).setPublishedOn(any(LocalDate.class));
        verify(chapterRepository).save(chapter);
    }

    @Test
    void publishChapter_noDebeCambiarPublishedOn_siYaExisteFecha() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Chapter saved = mock(Chapter.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        LocalDate existingDate = LocalDate.of(2026, Month.JANUARY, 1);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getPublishedOn()).thenReturn(existingDate);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getPublicationState()).thenReturn("published");

        ChapterPublicationStateResponse response = chapterService.publishChapter(5);

        assertEquals("published", response.publicationState());
        verify(chapter).setPublicationState("published");
        verify(chapter, never()).setPublishedOn(any(LocalDate.class));
    }

    @Test
    void unpublishChapter_deberiaCambiarEstadoADraft() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Chapter saved = mock(Chapter.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getPublicationState()).thenReturn("draft");

        ChapterPublicationStateResponse response = chapterService.unpublishChapter(5);

        assertEquals(5, response.id());
        assertEquals("draft", response.publicationState());

        verify(chapter).setPublicationState("draft");
        verify(chapterRepository).save(chapter);
    }

    @Test
    void archiveChapter_deberiaAsignarArchivedAtYGuardar() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Chapter saved = mock(Chapter.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.USER);

        when(chapterRepository.findById(5)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(chapterRepository.save(chapter)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getArchivedAt()).thenReturn(LocalDateTime.of(2026, Month.APRIL, 22, 13, 0));

        ChapterArchiveResponse response = chapterService.archiveChapter(5);

        assertEquals(5, response.id());
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 22, 13, 0), response.archivedAt());

        verify(chapter).setArchivedAt(any(LocalDateTime.class));
        verify(chapterRepository).save(chapter);
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}
