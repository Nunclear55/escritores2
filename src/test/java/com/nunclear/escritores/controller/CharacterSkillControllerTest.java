package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.AssignCharacterSkillRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterSkillRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.CharacterSkillService;
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

@WebMvcTest(CharacterSkillController.class)
class CharacterSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CharacterSkillService characterSkillService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void assignSkill_deberiaResponder200() throws Exception {
        AssignCharacterSkillRequest request = new AssignCharacterSkillRequest(11, 22, 7, "Nota");
        AssignCharacterSkillResponse response = new AssignCharacterSkillResponse(1, 11, 22, 7, "Nota");

        when(characterSkillService.assignSkill(eq(request))).thenReturn(response);

        mockMvc.perform(post("/character-skills")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storyCharacterId").value(11))
                .andExpect(jsonPath("$.skillId").value(22));
    }

    @Test
    void getSkillsByCharacter_deberiaResponder200() throws Exception {
        PageResponse<CharacterSkillForCharacterResponse> response = new PageResponse<>(
                List.of(new CharacterSkillForCharacterResponse(1, 22, "Magia", 7, "Nota")),
                0, 20, 1, 1
        );

        when(characterSkillService.getSkillsByCharacter(11, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/character-skills/character/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].skillName").value("Magia"));
    }

    @Test
    void getCharactersBySkill_deberiaResponder200() throws Exception {
        PageResponse<CharacterSkillForSkillResponse> response = new PageResponse<>(
                List.of(new CharacterSkillForSkillResponse(1, 11, "Alicia", 7)),
                0, 20, 1, 1
        );

        when(characterSkillService.getCharactersBySkill(22, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/character-skills/skill/22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].characterName").value("Alicia"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateRelation_deberiaResponder200() throws Exception {
        UpdateCharacterSkillRequest request = new UpdateCharacterSkillRequest(9, "Actualizado");
        UpdateCharacterSkillResponse response = new UpdateCharacterSkillResponse(1, 9, "Actualizado");

        when(characterSkillService.updateRelation(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/character-skills/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proficiency").value(9));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteRelation_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Relación eliminada correctamente");

        when(characterSkillService.deleteRelation(1)).thenReturn(response);

        mockMvc.perform(delete("/character-skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Relación eliminada correctamente"));

        verify(characterSkillService).deleteRelation(1);
    }

    @Test
    void assignSkill_deberiaResponder403_siNoAutenticado() throws Exception {
        AssignCharacterSkillRequest request = new AssignCharacterSkillRequest(11, 22, 7, "Nota");

        mockMvc.perform(post("/character-skills")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}