package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateIdeaRequest;
import com.nunclear.escritores.dto.request.UpdateIdeaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Idea;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.IdeaRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdeaService {

    private final IdeaRepository ideaRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateIdeaResponse createIdea(CreateIdeaRequest request) {
        Story story = getEditableStory(request.storyId());

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

        getEditableStory(idea.getStoryId());

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
        getEditableStory(storyId);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "updatedAt,desc" : sort);
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

        getEditableStory(idea.getStoryId());
        return idea;
    }

    private Story getEditableStory(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Historia no encontrada"));

        AppUser currentUser = getAuthenticatedUser();
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre esta historia");
        }

        return story;
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // Acceder directamente a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / programación insegura defensivamente.
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

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "createdAt" -> "createdAt";
            case "title" -> "title";
            default -> "updatedAt";
        };
    }
}