package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Arc;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.Volume;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ArcRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.repository.VolumeRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolumeService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String STORY_NOT_FOUND = "Historia no encontrada";
    private static final String SORT_POSITION_INDEX = "positionIndex";

    private final VolumeRepository volumeRepository;
    private final StoryRepository storyRepository;
    private final ArcRepository arcRepository;
    private final AppUserRepository appUserRepository;

    public CreateVolumeResponse createVolume(CreateVolumeRequest request) {
        Story story = getEditableStory(request.storyId());
        validateArcBelongsToStory(request.arcId(), story.getId());

        Volume volume = new Volume();
        volume.setStoryId(story.getId());
        volume.setArcId(request.arcId());
        volume.setTitle(request.title());
        volume.setPositionIndex(request.positionIndex());

        Volume saved = volumeRepository.save(volume);

        return new CreateVolumeResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getArcId(),
                saved.getTitle(),
                saved.getPositionIndex()
        );
    }

    public VolumeDetailResponse getVolumeById(Integer id) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volumen no encontrado"));

        Story story = storyRepository.findById(volume.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new VolumeDetailResponse(
                volume.getId(),
                volume.getStoryId(),
                volume.getArcId(),
                volume.getTitle(),
                volume.getPositionIndex()
        );
    }

    public PageResponse<VolumeListItemResponse> getVolumesByStory(Integer storyId, int page, int size, String sort) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? SORT_POSITION_INDEX + ",asc" : sort
        );

        Page<Volume> result = volumeRepository.findByStoryId(storyId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(volume -> new VolumeListItemResponse(
                                volume.getId(),
                                volume.getTitle(),
                                volume.getPositionIndex()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateVolumeResponse updateVolume(Integer id, UpdateVolumeRequest request) {
        Volume volume = getEditableVolume(id);
        validateArcBelongsToStory(request.arcId(), volume.getStoryId());

        volume.setTitle(request.title());
        volume.setArcId(request.arcId());
        volume.setPositionIndex(request.positionIndex());

        Volume saved = volumeRepository.save(volume);

        return new UpdateVolumeResponse(
                saved.getId(),
                saved.getTitle()
        );
    }

    public MessageResponse reorderVolumes(ReorderVolumesRequest request) {
        Story story = getEditableStory(request.storyId());

        Map<Integer, Integer> requestedPositions = request.items().stream()
                .collect(Collectors.toMap(ReorderVolumeItemRequest::volumeId, ReorderVolumeItemRequest::positionIndex));

        List<Volume> volumes = volumeRepository.findAllById(requestedPositions.keySet());

        if (volumes.size() != request.items().size()) {
            throw new BadRequestException("Uno o más volúmenes no existen");
        }

        for (Volume volume : volumes) {
            if (!volume.getStoryId().equals(story.getId())) {
                throw new BadRequestException("Todos los volúmenes deben pertenecer a la historia indicada");
            }
            volume.setPositionIndex(requestedPositions.get(volume.getId()));
        }

        volumeRepository.saveAll(volumes);

        return new MessageResponse("Volúmenes reordenados correctamente");
    }

    public MoveVolumeResponse moveVolume(Integer id, MoveVolumeRequest request) {
        Volume volume = getEditableVolume(id);

        Arc targetArc = arcRepository.findById(request.targetArcId())
                .orElseThrow(() -> new BadRequestException("El arco destino no existe"));

        if (!targetArc.getStoryId().equals(volume.getStoryId())) {
            throw new BadRequestException("El arco destino no pertenece a la misma historia");
        }

        volume.setArcId(targetArc.getId());
        volume.setPositionIndex(request.newPositionIndex());

        Volume saved = volumeRepository.save(volume);

        return new MoveVolumeResponse(
                saved.getId(),
                saved.getArcId(),
                saved.getPositionIndex()
        );
    }

    public MessageResponse deleteVolume(Integer id) {
        Volume volume = getEditableVolume(id);
        volumeRepository.delete(volume);
        return new MessageResponse("Volumen eliminado correctamente");
    }

    private Volume getEditableVolume(Integer id) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volumen no encontrado"));

        getEditableStory(volume.getStoryId());
        return volume;
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

    private void validateArcBelongsToStory(Integer arcId, Integer storyId) {
        if (arcId == null) {
            return;
        }

        Arc arc = arcRepository.findById(arcId)
                .orElseThrow(() -> new BadRequestException("El arco no existe"));

        if (!arc.getStoryId().equals(storyId)) {
            throw new BadRequestException("El arco no pertenece a la historia");
        }
    }

    private void validateReadAccess(Story story) {
        boolean publicReadable =
                "public".equalsIgnoreCase(story.getVisibilityState())
                        && "published".equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }

        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / falta de validación defensiva.
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
        // Mala práctica corregida:
        // catch vacío.
        // Tipo: swallowing exceptions / ocultamiento silencioso de errores.
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
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "title" -> "title";
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case SORT_POSITION_INDEX -> SORT_POSITION_INDEX;
            default -> SORT_POSITION_INDEX;
        };
    }
}