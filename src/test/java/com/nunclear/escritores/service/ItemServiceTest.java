package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateItemRequest;
import com.nunclear.escritores.dto.request.UpdateItemRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Item;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ItemRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ItemService itemService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createItem_deberiaCrearItem() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Item saved = mock(Item.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(itemRepository.save(any(Item.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getName()).thenReturn("Espada");

        CreateItemResponse response = itemService.createItem(
                new CreateItemRequest(10, "Espada", "Larga", 2, "unidad")
        );

        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Espada", response.name());

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        assertEquals("Espada", captor.getValue().getName());
        assertEquals(2, captor.getValue().getQuantity());
    }

    @Test
    void getItemById_deberiaRetornarItem_siHistoriaEsPublica() {
        Item item = mock(Item.class);
        Story story = mock(Story.class);

        when(itemRepository.findById(5)).thenReturn(Optional.of(item));
        when(item.getId()).thenReturn(5);
        when(item.getStoryId()).thenReturn(10);
        when(item.getName()).thenReturn("Espada");
        when(item.getDescription()).thenReturn("Larga");
        when(item.getQuantity()).thenReturn(2);
        when(item.getUnitName()).thenReturn("unidad");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        ItemDetailResponse response = itemService.getItemById(5);

        assertEquals(5, response.id());
        assertEquals("Espada", response.name());
    }

    @Test
    void getItemById_deberiaLanzarNotFound_siHistoriaNoVisible() {
        Item item = mock(Item.class);
        Story story = mock(Story.class);

        when(itemRepository.findById(5)).thenReturn(Optional.of(item));
        when(item.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> itemService.getItemById(5)
        );

        assertEquals("Historia no encontrada", ex.getMessage());
    }

    @Test
    void getItemsByStory_deberiaRetornarPagina() {
        Story story = mock(Story.class);
        Item item = mock(Item.class);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<Item> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        when(itemRepository.findByStoryWithNameFilter(eq(10), eq("esp"), any(Pageable.class))).thenReturn(page);

        when(item.getId()).thenReturn(1);
        when(item.getName()).thenReturn("Espada");

        PageResponse<ItemListItemResponse> response =
                itemService.getItemsByStory(10, "esp", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Espada", response.content().get(0).name());
    }

    @Test
    void updateItem_deberiaActualizarItem() {
        Item item = mock(Item.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Item saved = mock(Item.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(itemRepository.findById(5)).thenReturn(Optional.of(item));
        when(item.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(itemRepository.save(item)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getName()).thenReturn("Espada legendaria");

        UpdateItemResponse response = itemService.updateItem(
                5, new UpdateItemRequest("Espada legendaria", "Muy larga", 3, "unidad")
        );

        assertEquals(5, response.id());
        assertEquals("Espada legendaria", response.name());

        verify(item).setName("Espada legendaria");
        verify(item).setDescription("Muy larga");
        verify(item).setQuantity(3);
        verify(item).setUnitName("unidad");
    }

    @Test
    void deleteItem_deberiaEliminarItem() {
        Item item = mock(Item.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(itemRepository.findById(5)).thenReturn(Optional.of(item));
        when(item.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        MessageResponse response = itemService.deleteItem(5);

        assertEquals("Ítem eliminado correctamente", response.message());
        verify(itemRepository).delete(item);
    }

    @Test
    void createItem_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> itemService.createItem(new CreateItemRequest(10, "Espada", null, null, null))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}