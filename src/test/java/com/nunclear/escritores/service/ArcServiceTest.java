package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateArcRequest;
import com.nunclear.escritores.dto.request.ReorderArcItemRequest;
import com.nunclear.escritores.dto.request.ReorderArcsRequest;
import com.nunclear.escritores.dto.request.UpdateArcRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Arc;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ArcRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArcServiceTest {

    @Mock
    private ArcRepository arcRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ArcService arcService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createArc_deberiaCrearArco_siEsOwner() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Arc saved = mock(Arc.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.save(any(Arc.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getTitle()).thenReturn("Saga inicial");
        when(saved.getPositionIndex()).thenReturn(1);

        CreateArcRequest request = new CreateArcRequest(10, "Saga inicial", "Sub", 1);

        CreateArcResponse response = arcService.createArc(request);

        assertNotNull(response);
        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Saga inicial", response.title());
        assertEquals(1, response.positionIndex());

        ArgumentCaptor<Arc> captor = ArgumentCaptor.forClass(Arc.class);
        verify(arcRepository).save(captor.capture());

        Arc toSave = captor.getValue();
        assertEquals(10, toSave.getStoryId());
        assertEquals("Saga inicial", toSave.getTitle());
        assertEquals("Sub", toSave.getSubtitle());
        assertEquals(1, toSave.getPositionIndex());
    }

    @Test
    void createArc_deberiaLanzarUnauthorized_siNoEsOwnerNiAdminNiModerator() {
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
                () -> arcService.createArc(new CreateArcRequest(10, "Saga", "Sub", 1))
        );

        assertEquals("No tienes permisos sobre esta historia", ex.getMessage());
        verify(arcRepository, never()).save(any());
    }

    @Test
    void getArcById_deberiaRetornarArc_siHistoriaEsPublica() {
        Arc arc = mock(Arc.class);
        Story story = mock(Story.class);

        when(arcRepository.findById(5)).thenReturn(Optional.of(arc));
        when(arc.getId()).thenReturn(5);
        when(arc.getStoryId()).thenReturn(10);
        when(arc.getTitle()).thenReturn("Saga");
        when(arc.getSubtitle()).thenReturn("Sub");
        when(arc.getPositionIndex()).thenReturn(1);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        ArcDetailResponse response = arcService.getArcById(5);

        assertEquals(5, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Saga", response.title());
        assertEquals("Sub", response.subtitle());
        assertEquals(1, response.positionIndex());
    }

    @Test
    void getArcById_deberiaLanzarNotFound_siHistoriaPrivadaYNoAutenticado() {
        Arc arc = mock(Arc.class);
        Story story = mock(Story.class);

        when(arcRepository.findById(5)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> arcService.getArcById(5)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getArcsByStory_deberiaRetornarPagina() {
        Story story = mock(Story.class);
        Arc arc = mock(Arc.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<Arc> page = new PageImpl<>(List.of(arc), PageRequest.of(0, 20), 1);
        when(arcRepository.findByStoryId(eq(10), any(Pageable.class))).thenReturn(page);

        when(arc.getId()).thenReturn(1);
        when(arc.getTitle()).thenReturn("Saga 1");
        when(arc.getPositionIndex()).thenReturn(1);

        PageResponse<ArcListItemResponse> response = arcService.getArcsByStory(10, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Saga 1", response.content().get(0).title());
        assertEquals(1, response.totalElements());
    }

    @Test
    void updateArc_deberiaActualizarArc() {
        Arc arc = mock(Arc.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Arc saved = mock(Arc.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(arcRepository.findById(5)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.save(arc)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getTitle()).thenReturn("Nuevo título");

        UpdateArcRequest request = new UpdateArcRequest("Nuevo título", "Nuevo subtítulo", 2);

        UpdateArcResponse response = arcService.updateArc(5, request);

        assertEquals(5, response.id());
        assertEquals("Nuevo título", response.title());

        verify(arc).setTitle("Nuevo título");
        verify(arc).setSubtitle("Nuevo subtítulo");
        verify(arc).setPositionIndex(2);
    }

    @Test
    void reorderArcs_deberiaActualizarPosiciones() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Arc arc1 = mock(Arc.class);
        Arc arc2 = mock(Arc.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(arc1.getId()).thenReturn(100);
        when(arc1.getStoryId()).thenReturn(10);

        when(arc2.getId()).thenReturn(101);
        when(arc2.getStoryId()).thenReturn(10);

        when(arcRepository.findAllById(any())).thenReturn(List.of(arc1, arc2));

        ReorderArcsRequest request = new ReorderArcsRequest(
                10,
                List.of(
                        new ReorderArcItemRequest(100, 2),
                        new ReorderArcItemRequest(101, 1)
                )
        );

        MessageResponse response = arcService.reorderArcs(request);

        assertEquals("Arcos reordenados correctamente", response.message());
        verify(arc1).setPositionIndex(2);
        verify(arc2).setPositionIndex(1);
        verify(arcRepository).saveAll(anyList());
    }

    @Test
    void reorderArcs_deberiaLanzarBadRequest_siFaltanArcos() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findAllById(any())).thenReturn(List.of());

        ReorderArcsRequest request = new ReorderArcsRequest(
                10,
                List.of(new ReorderArcItemRequest(100, 1))
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> arcService.reorderArcs(request)
        );

        assertEquals("Uno o más arcos no existen", ex.getMessage());
    }

    @Test
    void reorderArcs_deberiaLanzarBadRequest_siArcNoPerteneceALaHistoria() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Arc arc = mock(Arc.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(arc.getId()).thenReturn(100);
        when(arc.getStoryId()).thenReturn(999);

        when(arcRepository.findAllById(any())).thenReturn(List.of(arc));

        ReorderArcsRequest request = new ReorderArcsRequest(
                10,
                List.of(new ReorderArcItemRequest(100, 1))
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> arcService.reorderArcs(request)
        );

        assertEquals("Todos los arcos deben pertenecer a la historia indicada", ex.getMessage());
    }

    @Test
    void deleteArc_deberiaEliminarArc() {
        Arc arc = mock(Arc.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(arcRepository.findById(5)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        MessageResponse response = arcService.deleteArc(5);

        assertEquals("Arco eliminado correctamente", response.message());
        verify(arcRepository).delete(arc);
    }

    @Test
    void getAuthenticatedUser_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> arcService.createArc(new CreateArcRequest(10, "Saga", "Sub", 1))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}