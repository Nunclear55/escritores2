package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateItemRequest;
import com.nunclear.escritores.dto.request.UpdateItemRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createItem_deberiaResponder200() throws Exception {
        CreateItemRequest request = new CreateItemRequest(10, "Espada", "Larga", 2, "unidad");
        CreateItemResponse response = new CreateItemResponse(1, 10, "Espada");

        when(itemService.createItem(eq(request))).thenReturn(response);

        mockMvc.perform(post("/items")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Espada"));
    }

    @Test
    void getItemById_deberiaResponder200() throws Exception {
        ItemDetailResponse response = new ItemDetailResponse(1, 10, "Espada", "Larga", 2, "unidad");

        when(itemService.getItemById(1)).thenReturn(response);

        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Espada"))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void getItemsByStory_deberiaResponder200() throws Exception {
        PageResponse<ItemListItemResponse> response = new PageResponse<>(
                List.of(new ItemListItemResponse(1, "Espada")),
                0, 20, 1, 1
        );

        when(itemService.getItemsByStory(10, "esp", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/items/story/10").param("name", "esp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Espada"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateItem_deberiaResponder200() throws Exception {
        UpdateItemRequest request = new UpdateItemRequest("Espada legendaria", "Muy larga", 3, "unidad");
        UpdateItemResponse response = new UpdateItemResponse(1, "Espada legendaria");

        when(itemService.updateItem(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/items/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Espada legendaria"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteItem_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Ítem eliminado correctamente");

        when(itemService.deleteItem(1)).thenReturn(response);

        mockMvc.perform(delete("/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ítem eliminado correctamente"));

        verify(itemService).deleteItem(1);
    }

    @Test
    void createItem_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateItemRequest request = new CreateItemRequest(10, "Espada", "Larga", 2, "unidad");

        mockMvc.perform(post("/items")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createItem_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "name": "",
                  "description": "Larga",
                  "quantity": 2,
                  "unitName": "unidad"
                }
                """;

        mockMvc.perform(post("/items")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}