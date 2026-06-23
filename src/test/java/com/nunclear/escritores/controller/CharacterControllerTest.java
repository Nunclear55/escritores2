package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateCharacterRequest;
import com.nunclear.escritores.dto.request.UpdateCharacterRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.CharacterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(CharacterController.class)
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CharacterService characterService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createCharacter_deberiaResponder200() throws Exception {
        CreateCharacterRequest request = new CreateCharacterRequest(
                10,
                "Alicia",
                "Descripción",
                "protagonist",
                "Guerrera",
                "Magia",
                20,
                LocalDate.of(2005, 1, 1),
                true,
                List.of("hero", "mage"),
                "https://img.test/alicia.jpg"
        );

        CreateCharacterResponse response = new CreateCharacterResponse(1, 10, "Alicia", "protagonist");

        when(characterService.createCharacter(eq(request))).thenReturn(response);

        mockMvc.perform(post("/characters")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.name").value("Alicia"))
                .andExpect(jsonPath("$.characterRoleName").value("protagonist"));
    }

    @Test
    void getCharacterById_deberiaResponder200() throws Exception {
        CharacterDetailResponse response = new CharacterDetailResponse(
                1, 10, "Alicia", "Descripción", "protagonist", "Guerrera"
        );

        when(characterService.getCharacterById(1)).thenReturn(response);

        mockMvc.perform(get("/characters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.name").value("Alicia"))
                .andExpect(jsonPath("$.characterRoleName").value("protagonist"));
    }

    @Test
    void getCharactersByStory_deberiaResponder200() throws Exception {
        PageResponse<CharacterListItemResponse> response = new PageResponse<>(
                List.of(new CharacterListItemResponse(1, "Alicia", "protagonist")),
                0, 20, 1, 1
        );

        when(characterService.getCharactersByStory(10, true, "protagonist", 0, 20, null))
                .thenReturn(response);

        mockMvc.perform(get("/characters/story/10")
                        .param("isAlive", "true")
                        .param("characterRoleName", "protagonist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alicia"))
                .andExpect(jsonPath("$.content[0].characterRoleName").value("protagonist"));
    }

    @Test
    void searchCharacters_deberiaResponder200() throws Exception {
        PageResponse<CharacterSearchItemResponse> response = new PageResponse<>(
                List.of(new CharacterSearchItemResponse(1, "Alicia", 10)),
                0, 20, 1, 1
        );

        when(characterService.searchCharacters("ali", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/characters/search")
                        .param("q", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alicia"))
                .andExpect(jsonPath("$.content[0].storyId").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCharacter_deberiaResponder200() throws Exception {
        UpdateCharacterRequest request = new UpdateCharacterRequest(
                "Alicia actualizada",
                "Nueva descripción",
                "support",
                "Arquera",
                "Puntería",
                21,
                LocalDate.of(2004, 1, 1),
                true,
                List.of("support", "archer"),
                "https://img.test/new.jpg"
        );

        UpdateCharacterResponse response = new UpdateCharacterResponse(
                1, "Alicia actualizada", LocalDateTime.of(2026, 4, 22, 12, 0)
        );

        when(characterService.updateCharacter(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/characters/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alicia actualizada"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteCharacter_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Personaje eliminado correctamente");

        when(characterService.deleteCharacter(1)).thenReturn(response);

        mockMvc.perform(delete("/characters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Personaje eliminado correctamente"));

        verify(characterService).deleteCharacter(1);
    }

    @Test
    void createCharacter_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateCharacterRequest request = new CreateCharacterRequest(
                10, "Alicia", null, null, null, null,
                null, null, null, null, null
        );

        mockMvc.perform(post("/characters")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCharacter_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "name": "",
                  "description": "Descripción",
                  "characterRoleName": "protagonist",
                  "profession": "Guerrera",
                  "ability": "Magia",
                  "age": 20,
                  "birthDate": "2005-01-01",
                  "isAlive": true,
                  "rolesJson": ["hero"],
                  "imageUrl": "https://img.test/alicia.jpg"
                }
                """;

        mockMvc.perform(post("/characters")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}