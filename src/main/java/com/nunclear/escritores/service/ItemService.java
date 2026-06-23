package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateItemRequest;
import com.nunclear.escritores.dto.request.UpdateItemRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Item;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ItemRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

    // Mala práctica corregida:
    // literal duplicado.
    // Tipo: duplicación de cadenas mágicas / menor mantenibilidad.
    private static final String STORY_NOT_FOUND = "Historia no encontrada";

    private final ItemRepository itemRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateItemResponse createItem(CreateItemRequest request) {
        Story story = getEditableStory(request.storyId());

        Item item = new Item();
        item.setStoryId(story.getId());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitName(request.unitName());

        Item saved = itemRepository.save(item);

        return new CreateItemResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getName()
        );
    }

    public ItemDetailResponse getItemById(Integer id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        Story story = storyRepository.findById(item.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        return new ItemDetailResponse(
                item.getId(),
                item.getStoryId(),
                item.getName(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitName()
        );
    }

    public PageResponse<ItemListItemResponse> getItemsByStory(
            Integer storyId,
            String name,
            int page,
            int size,
            String sort
    ) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        validateReadAccess(story);

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort);
        Page<Item> result = itemRepository.findByStoryWithNameFilter(storyId, name, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(item -> new ItemListItemResponse(
                                item.getId(),
                                item.getName()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateItemResponse updateItem(Integer id, UpdateItemRequest request) {
        Item item = getEditableItem(id);

        item.setName(request.name());
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitName(request.unitName());

        Item saved = itemRepository.save(item);

        return new UpdateItemResponse(
                saved.getId(),
                saved.getName()
        );
    }

    public MessageResponse deleteItem(Integer id) {
        Item item = getEditableItem(id);
        itemRepository.delete(item);
        return new MessageResponse("Ítem eliminado correctamente");
    }

    private Item getEditableItem(Integer id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        getEditableStory(item.getStoryId());
        return item;
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
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / validación defensiva insuficiente.
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
        // try/catch vacío.
        // Tipo: swallowing exceptions / ocultar errores.
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
            default -> "name";
        };
    }
}