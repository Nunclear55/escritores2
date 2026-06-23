package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateArcRequest;
import com.nunclear.escritores.dto.request.ReorderArcItemRequest;
import com.nunclear.escritores.dto.request.ReorderArcsRequest;
import com.nunclear.escritores.dto.request.UpdateArcRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.ArcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArcController.class)
class ArcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ArcService arcService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createArc_deberiaResponder200() throws Exception {
        CreateArcRequest request = new CreateArcRequest(10, "Saga 1", "Sub", 1);
        CreateArcResponse response = new CreateArcResponse(1, 10, "Saga 1", 1);

        when(arcService.createArc(eq(request))).thenReturn(response);

        mockMvc.perform(post("/arcs")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.title").value("Saga 1"))
                .andExpect(jsonPath("$.positionIndex").value(1));
    }

    @Test
    void getArcById_deberiaResponder200() throws Exception {
        ArcDetailResponse response = new ArcDetailResponse(1, 10, "Saga 1", "Sub", 1);

        when(arcService.getArcById(1)).thenReturn(response);

        mockMvc.perform(get("/arcs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.title").value("Saga 1"));
    }

    @Test
    void getArcsByStory_deberiaResponder200() throws Exception {
        PageResponse<ArcListItemResponse> response = new PageResponse<>(
                List.of(new ArcListItemResponse(1, "Saga 1", 1)),
                0, 20, 1, 1
        );

        when(arcService.getArcsByStory(10, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/arcs/story/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Saga 1"))
                .andExpect(jsonPath("$.content[0].positionIndex").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateArc_deberiaResponder200() throws Exception {
        UpdateArcRequest request = new UpdateArcRequest("Nuevo título", "Nuevo sub", 2);
        UpdateArcResponse response = new UpdateArcResponse(1, "Nuevo título");

        when(arcService.updateArc(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/arcs/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nuevo título"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void reorderArcs_deberiaResponder200() throws Exception {
        ReorderArcsRequest request = new ReorderArcsRequest(
                10,
                List.of(
                        new ReorderArcItemRequest(1, 2),
                        new ReorderArcItemRequest(2, 1)
                )
        );

        MessageResponse response = new MessageResponse("Arcos reordenados correctamente");

        when(arcService.reorderArcs(eq(request))).thenReturn(response);

        mockMvc.perform(post("/arcs/reorder")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Arcos reordenados correctamente"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteArc_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Arco eliminado correctamente");

        when(arcService.deleteArc(1)).thenReturn(response);

        mockMvc.perform(delete("/arcs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Arco eliminado correctamente"));

        verify(arcService).deleteArc(1);
    }

    @Test
    void createArc_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateArcRequest request = new CreateArcRequest(10, "Saga 1", "Sub", 1);

        mockMvc.perform(post("/arcs")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createArc_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "title": "",
                  "subtitle": "Sub",
                  "positionIndex": 1
                }
                """;

        mockMvc.perform(post("/arcs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}