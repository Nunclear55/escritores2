package com.nunclear.escritores.controller;

import com.nunclear.escritores.dto.request.CreateItemRequest;
import com.nunclear.escritores.dto.request.UpdateItemRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public CreateItemResponse createItem(@Valid @RequestBody CreateItemRequest request) {
        return itemService.createItem(request);
    }

    @GetMapping("/{id}")
    public ItemDetailResponse getItemById(@PathVariable Integer id) {
        return itemService.getItemById(id);
    }

    @GetMapping("/story/{storyId}")
    public PageResponse<ItemListItemResponse> getItemsByStory(
            @PathVariable Integer storyId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return itemService.getItemsByStory(storyId, name, page, size, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public UpdateItemResponse updateItem(@PathVariable Integer id, @Valid @RequestBody UpdateItemRequest request) {
        return itemService.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public MessageResponse deleteItem(@PathVariable Integer id) {
        return itemService.deleteItem(id);
    }
}