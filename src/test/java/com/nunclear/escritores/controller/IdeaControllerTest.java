package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateIdeaRequest;
import com.nunclear.escritores.dto.request.UpdateIdeaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.IdeaService;
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

@WebMvcTest(IdeaController.class)
class IdeaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private IdeaService ideaService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createIdea_deberiaResponder200() throws Exception {
        CreateIdeaRequest request = new CreateIdeaRequest(10, "Idea", "Contenido");
        CreateIdeaResponse response = new CreateIdeaResponse(1, 10, "Idea");

        when(ideaService.createIdea(eq(request))).thenReturn(response);

        mockMvc.perform(post("/ideas")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Idea"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getIdeaById_deberiaResponder200() throws Exception {
        IdeaDetailResponse response = new IdeaDetailResponse(1, 10, "Idea", "Contenido");

        when(ideaService.getIdeaById(1)).thenReturn(response);

        mockMvc.perform(get("/ideas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Idea"))
                .andExpect(jsonPath("$.content").value("Contenido"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getIdeasByStory_deberiaResponder200() throws Exception {
        PageResponse<IdeaListItemResponse> response = new PageResponse<>(
                List.of(new IdeaListItemResponse(1, "Idea base")),
                0, 20, 1, 1
        );

        when(ideaService.getIdeasByStory(10, "base", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/ideas/story/10").param("q", "base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Idea base"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateIdea_deberiaResponder200() throws Exception {
        UpdateIdeaRequest request = new UpdateIdeaRequest("Idea actualizada", "Nuevo contenido");
        UpdateIdeaResponse response = new UpdateIdeaResponse(1, "Idea actualizada");

        when(ideaService.updateIdea(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/ideas/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Idea actualizada"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteIdea_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Idea eliminada correctamente");

        when(ideaService.deleteIdea(1)).thenReturn(response);

        mockMvc.perform(delete("/ideas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Idea eliminada correctamente"));

        verify(ideaService).deleteIdea(1);
    }

    @Test
    void createIdea_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateIdeaRequest request = new CreateIdeaRequest(10, "Idea", "Contenido");

        mockMvc.perform(post("/ideas")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createIdea_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "title": "",
                  "content": "Contenido"
                }
                """;

        mockMvc.perform(post("/ideas")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}