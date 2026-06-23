package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.config.SecurityConfig;
import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.VolumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(StoryController.class)
class VolumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private VolumeService volumeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createVolume_deberiaResponder200() throws Exception {
        CreateVolumeRequest request = new CreateVolumeRequest(10, null, "Volumen 1", 1);
        CreateVolumeResponse response = new CreateVolumeResponse(1, 10, null, "Volumen 1", 1);

        when(volumeService.createVolume(eq(request))).thenReturn(response);

        mockMvc.perform(post("/volumes")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.title").value("Volumen 1"))
                .andExpect(jsonPath("$.positionIndex").value(1));
    }

    @Test
    void getVolumeById_deberiaResponder200() throws Exception {
        VolumeDetailResponse response = new VolumeDetailResponse(1, 10, 2, "Volumen 1", 1);

        when(volumeService.getVolumeById(1)).thenReturn(response);

        mockMvc.perform(get("/volumes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.arcId").value(2))
                .andExpect(jsonPath("$.title").value("Volumen 1"));
    }

    @Test
    void getVolumesByStory_deberiaResponder200() throws Exception {
        PageResponse<VolumeListItemResponse> response = new PageResponse<>(
                List.of(new VolumeListItemResponse(1, "Volumen 1", 1)),
                0, 20, 1, 1
        );

        when(volumeService.getVolumesByStory(10, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/volumes/story/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Volumen 1"))
                .andExpect(jsonPath("$.content[0].positionIndex").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateVolume_deberiaResponder200() throws Exception {
        UpdateVolumeRequest request = new UpdateVolumeRequest("Nuevo volumen", 7, 2);
        UpdateVolumeResponse response = new UpdateVolumeResponse(1, "Nuevo volumen");

        when(volumeService.updateVolume(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/volumes/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nuevo volumen"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void reorderVolumes_deberiaResponder200() throws Exception {
        ReorderVolumesRequest request = new ReorderVolumesRequest(
                10,
                List.of(
                        new ReorderVolumeItemRequest(1, 2),
                        new ReorderVolumeItemRequest(2, 1)
                )
        );

        MessageResponse response = new MessageResponse("Volúmenes reordenados correctamente");

        when(volumeService.reorderVolumes(eq(request))).thenReturn(response);

        mockMvc.perform(post("/volumes/reorder")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Volúmenes reordenados correctamente"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void moveVolume_deberiaResponder200() throws Exception {
        MoveVolumeRequest request = new MoveVolumeRequest(7, 4);
        MoveVolumeResponse response = new MoveVolumeResponse(1, 7, 4);

        when(volumeService.moveVolume(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(post("/volumes/1/move")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.arcId").value(7))
                .andExpect(jsonPath("$.positionIndex").value(4));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteVolume_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Volumen eliminado correctamente");

        when(volumeService.deleteVolume(1)).thenReturn(response);

        mockMvc.perform(delete("/volumes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Volumen eliminado correctamente"));

        verify(volumeService).deleteVolume(1);
    }

    @Test
    void createVolume_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateVolumeRequest request = new CreateVolumeRequest(10, null, "Volumen 1", 1);

        mockMvc.perform(post("/volumes")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createVolume_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "arcId": null,
                  "title": "",
                  "positionIndex": 1
                }
                """;

        mockMvc.perform(post("/volumes")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}