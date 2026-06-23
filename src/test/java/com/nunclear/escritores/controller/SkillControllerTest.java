package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateSkillRequest;
import com.nunclear.escritores.dto.request.UpdateSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.SkillService;
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

@WebMvcTest(SkillController.class)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SkillService skillService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createSkill_deberiaResponder200() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest(10, "Magia", "Desc", "Combate", 8);
        CreateSkillResponse response = new CreateSkillResponse(1, 10, "Magia", "Combate", 8);

        when(skillService.createSkill(eq(request))).thenReturn(response);

        mockMvc.perform(post("/skills")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.name").value("Magia"));
    }

    @Test
    void getSkillById_deberiaResponder200() throws Exception {
        SkillDetailResponse response = new SkillDetailResponse(1, 10, "Magia", "Combate", 8);

        when(skillService.getSkillById(1)).thenReturn(response);

        mockMvc.perform(get("/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Magia"));
    }

    @Test
    void getSkillsByStory_deberiaResponder200() throws Exception {
        PageResponse<SkillListItemResponse> response = new PageResponse<>(
                List.of(new SkillListItemResponse(1, "Magia", "Combate")),
                0, 20, 1, 1
        );

        when(skillService.getSkillsByStory(10, "Combate", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/skills/story/10").param("categoryName", "Combate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Magia"));
    }

    @Test
    void searchSkills_deberiaResponder200() throws Exception {
        PageResponse<SkillSearchItemResponse> response = new PageResponse<>(
                List.of(new SkillSearchItemResponse(1, "Magia")),
                0, 20, 1, 1
        );

        when(skillService.searchSkills("mag", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/skills/search").param("q", "mag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Magia"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateSkill_deberiaResponder200() throws Exception {
        UpdateSkillRequest request = new UpdateSkillRequest("Nueva magia", "Desc", "Mental", 9);
        UpdateSkillResponse response = new UpdateSkillResponse(1, "Nueva magia");

        when(skillService.updateSkill(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/skills/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nueva magia"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteSkill_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Habilidad eliminada correctamente");

        when(skillService.deleteSkill(1)).thenReturn(response);

        mockMvc.perform(delete("/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Habilidad eliminada correctamente"));

        verify(skillService).deleteSkill(1);
    }

    @Test
    void createSkill_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest(10, "Magia", null, null, null);

        mockMvc.perform(post("/skills")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createSkill_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "name": "",
                  "description": "Desc",
                  "categoryName": "Combate",
                  "levelValue": 8
                }
                """;

        mockMvc.perform(post("/skills")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}