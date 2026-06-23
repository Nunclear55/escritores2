package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateVolumeRequest;
import com.nunclear.escritores.dto.request.MoveVolumeRequest;
import com.nunclear.escritores.dto.request.ReorderVolumeItemRequest;
import com.nunclear.escritores.dto.request.ReorderVolumesRequest;
import com.nunclear.escritores.dto.request.UpdateVolumeRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Arc;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.Volume;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ArcRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolumeServiceTest {

    @Mock
    private VolumeRepository volumeRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private ArcRepository arcRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private VolumeService volumeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createVolume_deberiaCrearVolumen_siEsOwner() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume saved = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(volumeRepository.save(any(Volume.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getArcId()).thenReturn(null);
        when(saved.getTitle()).thenReturn("Volumen 1");
        when(saved.getPositionIndex()).thenReturn(1);

        CreateVolumeRequest request = new CreateVolumeRequest(10, null, "Volumen 1", 1);

        CreateVolumeResponse response = volumeService.createVolume(request);

        assertNotNull(response);
        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertNull(response.arcId());
        assertEquals("Volumen 1", response.title());
        assertEquals(1, response.positionIndex());

        ArgumentCaptor<Volume> captor = ArgumentCaptor.forClass(Volume.class);
        verify(volumeRepository).save(captor.capture());

        Volume toSave = captor.getValue();
        assertEquals(10, toSave.getStoryId());
        assertNull(toSave.getArcId());
        assertEquals("Volumen 1", toSave.getTitle());
        assertEquals(1, toSave.getPositionIndex());
    }

    @Test
    void createVolume_deberiaCrearVolumen_conArcValido() {
        Story story = mock(Story.class);
        Arc arc = mock(Arc.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume saved = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(5)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(10);

        when(volumeRepository.save(any(Volume.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getArcId()).thenReturn(5);
        when(saved.getTitle()).thenReturn("Volumen 1");
        when(saved.getPositionIndex()).thenReturn(1);

        CreateVolumeResponse response =
                volumeService.createVolume(new CreateVolumeRequest(10, 5, "Volumen 1", 1));

        assertEquals(5, response.arcId());
    }

    @Test
    void createVolume_deberiaLanzarBadRequest_siArcNoExiste() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(5)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> volumeService.createVolume(new CreateVolumeRequest(10, 5, "Volumen", 1))
        );

        assertEquals("El arco no existe", ex.getMessage());
        verify(volumeRepository, never()).save(any());
    }

    @Test
    void createVolume_deberiaLanzarBadRequest_siArcNoPerteneceALaHistoria() {
        Story story = mock(Story.class);
        Arc arc = mock(Arc.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(5)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(999);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> volumeService.createVolume(new CreateVolumeRequest(10, 5, "Volumen", 1))
        );

        assertEquals("El arco no pertenece a la historia", ex.getMessage());
    }

    @Test
    void getVolumeById_deberiaRetornarVolumen_siHistoriaEsPublica() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getId()).thenReturn(5);
        when(volume.getStoryId()).thenReturn(10);
        when(volume.getArcId()).thenReturn(2);
        when(volume.getTitle()).thenReturn("Volumen");
        when(volume.getPositionIndex()).thenReturn(1);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        VolumeDetailResponse response = volumeService.getVolumeById(5);

        assertEquals(5, response.id());
        assertEquals(10, response.storyId());
        assertEquals(2, response.arcId());
        assertEquals("Volumen", response.title());
    }

    @Test
    void getVolumeById_deberiaLanzarNotFound_siHistoriaPrivadaYNoAutenticado() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> volumeService.getVolumeById(5)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getVolumesByStory_deberiaRetornarPagina() {
        Story story = mock(Story.class);
        Volume volume = mock(Volume.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<Volume> page = new PageImpl<>(List.of(volume), PageRequest.of(0, 20), 1);
        when(volumeRepository.findByStoryId(eq(10), any(Pageable.class))).thenReturn(page);

        when(volume.getId()).thenReturn(1);
        when(volume.getTitle()).thenReturn("Volumen 1");
        when(volume.getPositionIndex()).thenReturn(1);

        PageResponse<VolumeListItemResponse> response = volumeService.getVolumesByStory(10, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Volumen 1", response.content().get(0).title());
    }

    @Test
    void updateVolume_deberiaActualizarVolume() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);
        Arc arc = mock(Arc.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume saved = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(7)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(10);

        when(volumeRepository.save(volume)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getTitle()).thenReturn("Nuevo volumen");

        UpdateVolumeResponse response =
                volumeService.updateVolume(5, new UpdateVolumeRequest("Nuevo volumen", 7, 2));

        assertEquals(5, response.id());
        assertEquals("Nuevo volumen", response.title());

        verify(volume).setTitle("Nuevo volumen");
        verify(volume).setArcId(7);
        verify(volume).setPositionIndex(2);
    }

    @Test
    void reorderVolumes_deberiaActualizarPosiciones() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume vol1 = mock(Volume.class);
        Volume vol2 = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(vol1.getId()).thenReturn(100);
        when(vol1.getStoryId()).thenReturn(10);

        when(vol2.getId()).thenReturn(101);
        when(vol2.getStoryId()).thenReturn(10);

        when(volumeRepository.findAllById(any())).thenReturn(List.of(vol1, vol2));

        ReorderVolumesRequest request = new ReorderVolumesRequest(
                10,
                List.of(
                        new ReorderVolumeItemRequest(100, 2),
                        new ReorderVolumeItemRequest(101, 1)
                )
        );

        MessageResponse response = volumeService.reorderVolumes(request);

        assertEquals("Volúmenes reordenados correctamente", response.message());
        verify(vol1).setPositionIndex(2);
        verify(vol2).setPositionIndex(1);
        verify(volumeRepository).saveAll(anyList());
    }

    @Test
    void reorderVolumes_deberiaLanzarBadRequest_siFaltanVolumenes() {
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

        when(volumeRepository.findAllById(any())).thenReturn(List.of());

        ReorderVolumesRequest request = new ReorderVolumesRequest(
                10,
                List.of(new ReorderVolumeItemRequest(100, 1))
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> volumeService.reorderVolumes(request)
        );

        assertEquals("Uno o más volúmenes no existen", ex.getMessage());
    }

    @Test
    void reorderVolumes_deberiaLanzarBadRequest_siVolumenNoPerteneceALaHistoria() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume volume = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(volume.getId()).thenReturn(100);
        when(volume.getStoryId()).thenReturn(999);

        when(volumeRepository.findAllById(any())).thenReturn(List.of(volume));

        ReorderVolumesRequest request = new ReorderVolumesRequest(
                10,
                List.of(new ReorderVolumeItemRequest(100, 1))
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> volumeService.reorderVolumes(request)
        );

        assertEquals("Todos los volúmenes deben pertenecer a la historia indicada", ex.getMessage());
    }

    @Test
    void moveVolume_deberiaMoverAArcDestino() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);
        Arc arc = mock(Arc.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        Volume saved = mock(Volume.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(7)).thenReturn(Optional.of(arc));
        when(arc.getId()).thenReturn(7);
        when(arc.getStoryId()).thenReturn(10);

        when(volumeRepository.save(volume)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getArcId()).thenReturn(7);
        when(saved.getPositionIndex()).thenReturn(4);

        MoveVolumeResponse response = volumeService.moveVolume(5, new MoveVolumeRequest(7, 4));

        assertEquals(5, response.id());
        assertEquals(7, response.arcId());
        assertEquals(4, response.positionIndex());

        verify(volume).setArcId(7);
        verify(volume).setPositionIndex(4);
    }

    @Test
    void moveVolume_deberiaLanzarBadRequest_siArcoDestinoNoExiste() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(7)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> volumeService.moveVolume(5, new MoveVolumeRequest(7, 4))
        );

        assertEquals("El arco destino no existe", ex.getMessage());
    }

    @Test
    void moveVolume_deberiaLanzarBadRequest_siArcoDestinoNoPerteneceALaMismaHistoria() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);
        Arc arc = mock(Arc.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(arcRepository.findById(7)).thenReturn(Optional.of(arc));
        when(arc.getStoryId()).thenReturn(999);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> volumeService.moveVolume(5, new MoveVolumeRequest(7, 4))
        );

        assertEquals("El arco destino no pertenece a la misma historia", ex.getMessage());
    }

    @Test
    void deleteVolume_deberiaEliminarVolumen() {
        Volume volume = mock(Volume.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(volumeRepository.findById(5)).thenReturn(Optional.of(volume));
        when(volume.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        MessageResponse response = volumeService.deleteVolume(5);

        assertEquals("Volumen eliminado correctamente", response.message());
        verify(volumeRepository).delete(volume);
    }

    @Test
    void getAuthenticatedUser_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> volumeService.createVolume(new CreateVolumeRequest(10, null, "Volumen", 1))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}