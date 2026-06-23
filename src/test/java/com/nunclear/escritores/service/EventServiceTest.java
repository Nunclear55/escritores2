package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateEventRequest;
import com.nunclear.escritores.dto.request.UpdateEventRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private StoryEventRepository storyEventRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private StoryCharacterRepository storyCharacterRepository;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks
    private EventService eventService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEvent_deberiaCrearEvento() {
        Story story = mock(Story.class);
        Chapter chapter = mock(Chapter.class);
        StoryCharacter character = mock(StoryCharacter.class);
        AppUser user = mock(AppUser.class);
        StoryEvent saved = mock(StoryEvent.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyEventRepository.save(any(StoryEvent.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getChapterId()).thenReturn(3);
        when(saved.getTitle()).thenReturn("Batalla");

        CreateEventResponse response = eventService.createEvent(
                new CreateEventRequest(
                        10, 3, "Batalla", "Desc", LocalDate.of(2026, 1, 1), 5,
                        "combat", List.of("war"), List.of(11)
                )
        );

        assertEquals(100, response.id());
        assertEquals("Batalla", response.title());

        ArgumentCaptor<StoryEvent> captor = ArgumentCaptor.forClass(StoryEvent.class);
        verify(storyEventRepository).save(captor.capture());
        assertEquals("[\"war\"]", captor.getValue().getTagsJson());
        assertEquals("[11]", captor.getValue().getLinkedCharactersJson());
    }

    @Test
    void createEvent_deberiaLanzarBadRequest_siCapituloNoExiste() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(chapterRepository.findById(3)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> eventService.createEvent(new CreateEventRequest(
                        10, 3, "Batalla", null, null, null, null, null, null
                ))
        );

        assertEquals("El capítulo no existe", ex.getMessage());
    }

    @Test
    void getEventById_deberiaRetornarEvento_siHistoriaEsPublica() {
        StoryEvent event = mock(StoryEvent.class);
        Story story = mock(Story.class);

        when(storyEventRepository.findById(5)).thenReturn(Optional.of(event));
        when(event.getStoryId()).thenReturn(10);
        when(event.getId()).thenReturn(5);
        when(event.getTitle()).thenReturn("Batalla");
        when(event.getDescription()).thenReturn("Desc");
        when(event.getImportance()).thenReturn(5);
        when(event.getEventKind()).thenReturn("combat");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        EventDetailResponse response = eventService.getEventById(5);

        assertEquals(5, response.id());
        assertEquals("Batalla", response.title());
    }

    @Test
    void getEventsByStory_deberiaRetornarPagina() {
        Story story = mock(Story.class);
        StoryEvent event = mock(StoryEvent.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<StoryEvent> page = new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1);
        when(storyEventRepository.findByStoryWithFilters(eq(10), eq("combat"), eq(5), any(Pageable.class)))
                .thenReturn(page);

        when(event.getId()).thenReturn(1);
        when(event.getTitle()).thenReturn("Batalla");
        when(event.getChapterId()).thenReturn(3);

        PageResponse<EventListItemResponse> response =
                eventService.getEventsByStory(10, "combat", 5, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Batalla", response.content().get(0).title());
    }

    @Test
    void getEventsByChapter_deberiaRetornarPagina_siCapituloEsVisible() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        StoryEvent event = mock(StoryEvent.class);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getArchivedAt()).thenReturn(null);
        when(chapter.getPublicationState()).thenReturn("published");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<StoryEvent> page = new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1);
        when(storyEventRepository.findByChapterId(eq(3), any(Pageable.class))).thenReturn(page);

        when(event.getId()).thenReturn(1);
        when(event.getTitle()).thenReturn("Batalla");
        when(event.getChapterId()).thenReturn(3);

        PageResponse<EventListItemResponse> response = eventService.getEventsByChapter(3, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Batalla", response.content().get(0).title());
    }

    @Test
    void searchEvents_deberiaRetornarSoloEventosVisibles() {
        StoryEvent visible = mock(StoryEvent.class);
        StoryEvent hidden = mock(StoryEvent.class);
        Story publicStory = mock(Story.class);
        Story privateStory = mock(Story.class);

        Page<StoryEvent> page = new PageImpl<>(List.of(visible, hidden), PageRequest.of(0, 20), 2);
        when(storyEventRepository.searchEvents(eq("bat"), eq("war"), any(Pageable.class))).thenReturn(page);

        when(visible.getId()).thenReturn(1);
        when(visible.getTitle()).thenReturn("Batalla");
        when(visible.getChapterId()).thenReturn(3);
        when(visible.getStoryId()).thenReturn(10);

        when(hidden.getId()).thenReturn(2);
        when(hidden.getTitle()).thenReturn("Masacre");
        when(hidden.getChapterId()).thenReturn(4);
        when(hidden.getStoryId()).thenReturn(20);

        when(storyRepository.findById(10)).thenReturn(Optional.of(publicStory));
        when(publicStory.getVisibilityState()).thenReturn("public");
        when(publicStory.getPublicationState()).thenReturn("published");
        when(publicStory.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(20)).thenReturn(Optional.of(privateStory));
        when(privateStory.getVisibilityState()).thenReturn("private");
        when(privateStory.getPublicationState()).thenReturn("draft");
        when(privateStory.getArchivedAt()).thenReturn(null);

        PageResponse<EventListItemResponse> response = eventService.searchEvents("bat", "war", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Batalla", response.content().get(0).title());
        assertEquals(2, response.totalElements());
    }

    @Test
    void updateEvent_deberiaActualizarEvento() {
        StoryEvent event = mock(StoryEvent.class);
        Story story = mock(Story.class);
        StoryCharacter character = mock(StoryCharacter.class);
        AppUser user = mock(AppUser.class);
        StoryEvent saved = mock(StoryEvent.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyEventRepository.findById(5)).thenReturn(Optional.of(event));
        when(event.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyCharacterRepository.findById(11)).thenReturn(Optional.of(character));
        when(character.getStoryId()).thenReturn(10);

        when(storyEventRepository.save(event)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getTitle()).thenReturn("Batalla final");

        UpdateEventResponse response = eventService.updateEvent(
                5,
                new UpdateEventRequest(
                        "Batalla final", "Desc", LocalDate.of(2026, 1, 2), 9,
                        "combat", List.of("final"), List.of(11)
                )
        );

        assertEquals(5, response.id());
        assertEquals("Batalla final", response.title());

        verify(event).setTitle("Batalla final");
        verify(event).setDescription("Desc");
        verify(event).setImportance(9);
        verify(event).setEventKind("combat");
        verify(event).setTagsJson("[\"final\"]");
        verify(event).setLinkedCharactersJson("[11]");
    }

    @Test
    void deleteEvent_deberiaEliminarEvento() {
        StoryEvent event = mock(StoryEvent.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyEventRepository.findById(5)).thenReturn(Optional.of(event));
        when(event.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        MessageResponse response = eventService.deleteEvent(5);

        assertEquals("Evento eliminado correctamente", response.message());
        verify(storyEventRepository).delete(event);
    }

    @Test
    void createEvent_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> eventService.createEvent(new CreateEventRequest(
                        10, null, "Batalla", null, null, null, null, null, null
                ))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    // ==================== ADDITIONAL COVERAGE TESTS ====================

    @Test
    void getEventsByChapter_deberiaRetornarVacio_siCapituloNoTieneEventos() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getArchivedAt()).thenReturn(null);
        when(chapter.getPublicationState()).thenReturn("published");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<StoryEvent> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(storyEventRepository.findByChapterId(eq(3), any(Pageable.class))).thenReturn(page);

        PageResponse<EventListItemResponse> response = eventService.getEventsByChapter(3, 0, 20, "createdAt,asc");

        assertEquals(0, response.content().size());
    }

    @Test
    void getEventsByStory_deberiaRetornarVacio_siHistoriaNoTieneEventos() {
        Story story = mock(Story.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<StoryEvent> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(storyEventRepository.findByStoryWithFilters(eq(10), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<EventListItemResponse> response =
                eventService.getEventsByStory(10, null, null, 0, 20, "eventOn,desc");

        assertEquals(0, response.content().size());
    }

    @Test
    void searchEvents_deberiaFiltrarEventosPrivados() {
        StoryEvent event1 = mock(StoryEvent.class);
        StoryEvent event2 = mock(StoryEvent.class);
        Story publicStory = mock(Story.class);
        Story privateBanned = mock(Story.class);

        Page<StoryEvent> page = new PageImpl<>(List.of(event1, event2), PageRequest.of(0, 20), 2);
        when(storyEventRepository.searchEvents(eq("test"), any(), any(Pageable.class))).thenReturn(page);

        when(event1.getId()).thenReturn(1);
        when(event1.getTitle()).thenReturn("Público");
        when(event1.getChapterId()).thenReturn(5);
        when(event1.getStoryId()).thenReturn(10);

        when(event2.getId()).thenReturn(2);
        when(event2.getTitle()).thenReturn("Privado");
        when(event2.getChapterId()).thenReturn(6);
        when(event2.getStoryId()).thenReturn(20);

        when(storyRepository.findById(10)).thenReturn(Optional.of(publicStory));
        when(publicStory.getVisibilityState()).thenReturn("public");
        when(publicStory.getPublicationState()).thenReturn("published");
        when(publicStory.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(20)).thenReturn(Optional.of(privateBanned));
        when(privateBanned.getVisibilityState()).thenReturn("private");

        PageResponse<EventListItemResponse> response = eventService.searchEvents("test", null, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Público", response.content().get(0).title());
    }

    @Test
    void searchEvents_deberiaFiltrarEventosDeHistoriasEliminadas() {
        StoryEvent event1 = mock(StoryEvent.class);
        StoryEvent event2 = mock(StoryEvent.class);
        Story validStory = mock(Story.class);

        Page<StoryEvent> page = new PageImpl<>(List.of(event1, event2), PageRequest.of(0, 20), 2);
        when(storyEventRepository.searchEvents(eq("event"), any(), any(Pageable.class))).thenReturn(page);

        when(event1.getId()).thenReturn(1);
        when(event1.getTitle()).thenReturn("Valid");
        when(event1.getChapterId()).thenReturn(5);
        when(event1.getStoryId()).thenReturn(10);

        when(event2.getId()).thenReturn(2);
        when(event2.getTitle()).thenReturn("Deleted");
        when(event2.getChapterId()).thenReturn(6);
        when(event2.getStoryId()).thenReturn(999);

        when(storyRepository.findById(10)).thenReturn(Optional.of(validStory));
        when(validStory.getVisibilityState()).thenReturn("public");
        when(validStory.getPublicationState()).thenReturn("published");
        when(validStory.getArchivedAt()).thenReturn(null);

        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        PageResponse<EventListItemResponse> response = eventService.searchEvents("event", null, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Valid", response.content().get(0).title());
    }

    @Test
    void getEventById_deberiaLanzarResourceNotFound_siEventoNoExiste() {
        when(storyEventRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventById(999)
        );

        assertEquals("Evento no encontrado", ex.getMessage());
    }

    @Test
    void getEventById_deberiaLanzarResourceNotFound_siHistoriaNoExiste() {
        StoryEvent event = mock(StoryEvent.class);

        when(storyEventRepository.findById(5)).thenReturn(Optional.of(event));
        when(event.getStoryId()).thenReturn(999);
        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventById(5)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getEventsByStory_deberiaLanzarResourceNotFound_siHistoriaNoExiste() {
        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventsByStory(999, null, null, 0, 20, null)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getEventsByChapter_deberiaLanzarResourceNotFound_siCapituloNoExiste() {
        when(chapterRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventsByChapter(999, 0, 20, null)
        );

        assertEquals("Capítulo no encontrado", ex.getMessage());
    }

    @Test
    void getEventsByChapter_deberiaLanzarResourceNotFound_siHistoriaNoExiste() {
        Chapter chapter = mock(Chapter.class);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(999);
        when(storyRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventsByChapter(3, 0, 20, null)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void updateEvent_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> eventService.updateEvent(5, new UpdateEventRequest(
                        "Título", "Desc", null, null, null, null, null
                ))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void deleteEvent_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> eventService.deleteEvent(5)
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void deleteEvent_deberiaLanzarResourceNotFound_siEventoNoExiste() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyEventRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.deleteEvent(999)
        );

        assertEquals("Evento no encontrado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}
