package com.nunclear.escritores.service;


import com.nunclear.escritores.dto.request.CreateIdeaRequest;
import com.nunclear.escritores.dto.request.UpdateIdeaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.Idea;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.IdeaRepository;
import com.nunclear.escritores.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class IdeaService {

    private final IdeaRepository ideaRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateIdeaResponse createIdea(CreateIdeaRequest request) {
        Story story = StoryAccessUtils.getEditableStory(request.storyId(), storyRepository, appUserRepository);

        Idea idea = new Idea();
        idea.setStoryId(story.getId());
        idea.setTitle(request.title());
        idea.setContent(request.content());

        Idea saved = ideaRepository.save(idea);

        return new CreateIdeaResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getTitle()
        );
    }

    public IdeaDetailResponse getIdeaById(Integer id) {
        Idea idea = ideaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Idea no encontrada"));

        StoryAccessUtils.getEditableStory(idea.getStoryId(), storyRepository, appUserRepository);

        return new IdeaDetailResponse(
                idea.getId(),
                idea.getStoryId(),
                idea.getTitle(),
                idea.getContent()
        );
    }

    public PageResponse<IdeaListItemResponse> getIdeasByStory(
            Integer storyId,
            String q,
            int page,
            int size,
            String sort
    ) {
        StoryAccessUtils.getEditableStory(storyId, storyRepository, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "updatedAt,desc" : sort, "updatedAt", "createdAt", "title");
        Page<Idea> result = ideaRepository.findByStoryWithSearch(storyId, q, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(idea -> new IdeaListItemResponse(
                                idea.getId(),
                                idea.getTitle()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateIdeaResponse updateIdea(Integer id, UpdateIdeaRequest request) {
        Idea idea = getEditableIdea(id);

        idea.setTitle(request.title());
        idea.setContent(request.content());

        Idea saved = ideaRepository.save(idea);

        return new UpdateIdeaResponse(
                saved.getId(),
                saved.getTitle()
        );
    }

    public MessageResponse deleteIdea(Integer id) {
        Idea idea = getEditableIdea(id);
        ideaRepository.delete(idea);
        return new MessageResponse("Idea eliminada correctamente");
    }

    private Idea getEditableIdea(Integer id) {
        Idea idea = ideaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Idea no encontrada"));

        StoryAccessUtils.getEditableStory(idea.getStoryId(), storyRepository, appUserRepository);
        return idea;
    }

}