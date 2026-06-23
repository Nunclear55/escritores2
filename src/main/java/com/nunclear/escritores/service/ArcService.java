package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateArcRequest;
import com.nunclear.escritores.dto.request.ReorderArcItemRequest;
import com.nunclear.escritores.dto.request.ReorderArcsRequest;
import com.nunclear.escritores.dto.request.UpdateArcRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Arc;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ArcRepository;
import com.nunclear.escritores.repository.StoryRepository;
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
public class ArcService {

    private static final String STORY_NOT_FOUND = "Historia no encontrada";
    private static final String SORT_POSITION_INDEX = "positionIndex";
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_UPDATED_AT = "updatedAt";
    private static final String SORT_TITLE = "title";

    private final ArcRepository arcRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateArcResponse createArc(CreateArcRequest request) {
        Story story = getEditableStory(request.storyId());

        Arc arc = new Arc();
        arc.setStoryId(story.getId());
        arc.setTitle(request.title());
        arc.setSubtitle(request.subtitle());
        arc.setPositionIndex(request.positionIndex());

        Arc saved = arcRepository.save(arc);

        return new CreateArcResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getTitle(),
                saved.getPositionIndex()
        );
    }

    public ArcDetailResponse getArcById(Integer id) {
        Arc arc = arcRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Arco no encontrado"));

        Story story = storyRepository.findById(arc.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new ArcDetailResponse(
                arc.getId(),
                arc.getStoryId(),
                arc.getTitle(),
                arc.getSubtitle(),
                arc.getPositionIndex()
        );
    }

    public PageResponse<ArcListItemResponse> getArcsByStory(Integer storyId, int page, int size, String sort) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? SORT_POSITION_INDEX + ",asc" : sort
        );

        Page<Arc> result = arcRepository.findByStoryId(storyId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(arc -> new ArcListItemResponse(
                                arc.getId(),
                                arc.getTitle(),
                                arc.getPositionIndex()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateArcResponse updateArc(Integer id, UpdateArcRequest request) {
        Arc arc = getEditableArc(id);

        arc.setTitle(request.title());
        arc.setSubtitle(request.subtitle());
        arc.setPositionIndex(request.positionIndex());

        Arc saved = arcRepository.save(arc);

        return new UpdateArcResponse(
                saved.getId(),
                saved.getTitle()
        );
    }

    public MessageResponse reorderArcs(ReorderArcsRequest request) {
        Story story = getEditableStory(request.storyId());

        Map<Integer, Integer> requestedPositions = request.items().stream()
                .collect(Collectors.toMap(ReorderArcItemRequest::arcId, ReorderArcItemRequest::positionIndex));

        List<Arc> arcs = arcRepository.findAllById(requestedPositions.keySet());

        if (arcs.size() != request.items().size()) {
            throw new BadRequestException("Uno o más arcos no existen");
        }

        for (Arc arc : arcs) {
            if (!arc.getStoryId().equals(story.getId())) {
                throw new BadRequestException("Todos los arcos deben pertenecer a la historia indicada");
            }
            arc.setPositionIndex(requestedPositions.get(arc.getId()));
        }

        arcRepository.saveAll(arcs);

        return new MessageResponse("Arcos reordenados correctamente");
    }

    public MessageResponse deleteArc(Integer id) {
        Arc arc = getEditableArc(id);
        arcRepository.delete(arc);
        return new MessageResponse("Arco eliminado correctamente");
    }

    private Arc getEditableArc(Integer id) {
        Arc arc = arcRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Arco no encontrado"));

        getEditableStory(arc.getStoryId());
        return arc;
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
            case SORT_TITLE -> SORT_TITLE;
            case SORT_CREATED_AT -> SORT_CREATED_AT;
            case SORT_UPDATED_AT -> SORT_UPDATED_AT;
            case SORT_POSITION_INDEX -> SORT_POSITION_INDEX;
            default -> SORT_POSITION_INDEX;
        };
    }
}