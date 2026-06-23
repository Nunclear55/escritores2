package com.nunclear.escritores.service;


import com.nunclear.escritores.dto.request.CreateItemRequest;
import com.nunclear.escritores.dto.request.UpdateItemRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.Item;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ItemRepository;
import com.nunclear.escritores.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class ItemService {

    // Mala práctica corregida:
    // literal duplicado.
    // Tipo: duplicación de cadenas mágicas / menor mantenibilidad.

    private final ItemRepository itemRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public CreateItemResponse createItem(CreateItemRequest request) {
        Story story = StoryAccessUtils.getEditableStory(request.storyId(), storyRepository, appUserRepository);

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

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
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "name,asc" : sort, "name", "createdAt", "updatedAt");
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

        StoryAccessUtils.getEditableStory(item.getStoryId(), storyRepository, appUserRepository);
        return item;
    }

}