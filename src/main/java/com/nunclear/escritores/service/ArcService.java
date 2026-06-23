package com.nunclear.escritores.service;


import com.nunclear.escritores.dto.request.CreateArcRequest;
import com.nunclear.escritores.dto.request.ReorderArcItemRequest;
import com.nunclear.escritores.dto.request.ReorderArcsRequest;
import com.nunclear.escritores.dto.request.UpdateArcRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.Arc;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ArcRepository;
import com.nunclear.escritores.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class ArcService {
    private static final String SORT_POSITION_INDEX = "positionIndex";
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_UPDATED_AT = "updatedAt";
    private static final String SORT_TITLE = "title";

    private final ArcRepository arcRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateArcResponse createArc(CreateArcRequest request) {
        Story story = StoryAccessUtils.getEditableStory(request.storyId(), storyRepository, appUserRepository);

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? SORT_POSITION_INDEX + ",asc" : sort, SORT_POSITION_INDEX, SORT_TITLE, SORT_CREATED_AT, SORT_UPDATED_AT);

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
        Story story = StoryAccessUtils.getEditableStory(request.storyId(), storyRepository, appUserRepository);

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

        StoryAccessUtils.getEditableStory(arc.getStoryId(), storyRepository, appUserRepository);
        return arc;
    }

}