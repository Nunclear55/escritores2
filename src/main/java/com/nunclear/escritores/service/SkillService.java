package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateSkillRequest;
import com.nunclear.escritores.dto.request.UpdateSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Skill;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.SkillRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkillService {

    // Mala práctica corregida:
    // repetición de literales ("magic strings").
    // Tipo: duplicación de cadenas / baja mantenibilidad.
    private static final String STORY_NOT_FOUND = "Historia no encontrada";

    private final SkillRepository skillRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateSkillResponse createSkill(CreateSkillRequest request) {
        Story story = getEditableStory(request.storyId());

        Skill skill = new Skill();
        skill.setStoryId(story.getId());
        skill.setName(request.name());
        skill.setDescription(request.description());
        skill.setCategoryName(request.categoryName());
        skill.setLevelValue(request.levelValue());

        Skill saved = skillRepository.save(skill);

        return new CreateSkillResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getName(),
                saved.getCategoryName(),
                saved.getLevelValue()
        );
    }

    public SkillDetailResponse getSkillById(Integer id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habilidad no encontrada"));

        Story story = storyRepository.findById(skill.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new SkillDetailResponse(
                skill.getId(),
                skill.getStoryId(),
                skill.getName(),
                skill.getCategoryName(),
                skill.getLevelValue()
        );
    }

    public PageResponse<SkillListItemResponse> getSkillsByStory(
            Integer storyId,
            String categoryName,
            int page,
            int size,
            String sort
    ) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort);
        Page<Skill> result = skillRepository.findByStoryWithFilters(storyId, categoryName, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(skill -> new SkillListItemResponse(
                                skill.getId(),
                                skill.getName(),
                                skill.getCategoryName()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<SkillSearchItemResponse> searchSkills(String q, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort);
        Page<Skill> result = skillRepository.searchByName(q == null ? "" : q, pageable);

        var content = result.getContent().stream()
                .filter(skill -> {
                    Story story = storyRepository.findById(skill.getStoryId()).orElse(null);
                    return story != null && canReadStory(story);
                })
                .map(skill -> new SkillSearchItemResponse(
                        skill.getId(),
                        skill.getName()
                ))
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public UpdateSkillResponse updateSkill(Integer id, UpdateSkillRequest request) {
        Skill skill = getEditableSkill(id);

        skill.setName(request.name());
        skill.setDescription(request.description());
        skill.setCategoryName(request.categoryName());
        skill.setLevelValue(request.levelValue());

        Skill saved = skillRepository.save(skill);

        return new UpdateSkillResponse(saved.getId(), saved.getName());
    }

    public MessageResponse deleteSkill(Integer id) {
        Skill skill = getEditableSkill(id);
        skillRepository.delete(skill);
        return new MessageResponse("Habilidad eliminada correctamente");
    }

    private Skill getEditableSkill(Integer id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habilidad no encontrada"));
        getEditableStory(skill.getStoryId());
        return skill;
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
        if (!canReadStory(story)) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }
    }

    private boolean canReadStory(Story story) {
        boolean publicReadable =
                "public".equalsIgnoreCase(story.getVisibilityState())
                        && "published".equalsIgnoreCase(story.getPublicationState())
                        && story.getArchivedAt() == null;

        if (publicReadable) {
            return true;
        }

        AppUser currentUser = tryGetAuthenticatedUser();
        if (currentUser == null) {
            return false;
        }

        return story.getOwnerUserId().equals(currentUser.getId()) || isModeratorOrAdmin(currentUser);
    }

    private boolean isModeratorOrAdmin(AppUser user) {
        return "moderator".equals(user.getAccessLevel().name()) || "admin".equals(user.getAccessLevel().name());
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / falta de programación defensiva.
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
        // Tipo: swallowing exceptions / ocultar errores silenciosamente.
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
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "categoryName" -> "categoryName";
            default -> "name";
        };
    }
}