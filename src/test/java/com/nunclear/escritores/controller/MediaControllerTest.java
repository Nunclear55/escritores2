package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.ReplaceMediaRequest;
import com.nunclear.escritores.dto.request.UploadMediaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.MediaService;
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

@WebMvcTest(MediaController.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void uploadMedia_deberiaResponder200() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest(
                "imagen.png", "image", "desc", 3, "/files/imagen.png"
        );
        UploadMediaResponse response = new UploadMediaResponse(
                1, "imagen_1234abcd.png", "imagen.png", "image", 3
        );

        when(mediaService.uploadMedia(eq(request))).thenReturn(response);

        mockMvc.perform(post("/media/upload")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("imagen_1234abcd.png"))
                .andExpect(jsonPath("$.chapterId").value(3));
    }

    @Test
    void getMediaById_deberiaResponder200() throws Exception {
        MediaDetailResponse response = new MediaDetailResponse(
                1, "imagen_1234abcd.png", "imagen.png", "image", "desc", 3, "/files/imagen.png"
        );

        when(mediaService.getMediaById(1)).thenReturn(response);

        mockMvc.perform(get("/media/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("imagen_1234abcd.png"))
                .andExpect(jsonPath("$.mediaKind").value("image"));
    }

    @Test
    void getMediaByChapter_deberiaResponder200() throws Exception {
        PageResponse<MediaListItemResponse> response = new PageResponse<>(
                List.of(new MediaListItemResponse(1, "img.png", "image")),
                0, 20, 1, 1
        );

        when(mediaService.getMediaByChapter(3, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/media/chapter/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filename").value("img.png"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void replaceMedia_deberiaResponder200() throws Exception {
        ReplaceMediaRequest request = new ReplaceMediaRequest("nuevo.jpg", "desc nueva", "/files/nuevo.jpg");
        ReplaceMediaResponse response = new ReplaceMediaResponse(1, "nuevo_1234abcd.jpg", "desc nueva");

        when(mediaService.replaceMedia(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/media/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("nuevo_1234abcd.jpg"));
    }

    @Test
    void downloadMedia_deberiaResponder200() throws Exception {
        MediaDownloadResponse response = new MediaDownloadResponse("/files/doc.pdf");

        when(mediaService.downloadMedia(1)).thenReturn(response);

        mockMvc.perform(get("/media/1/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("/files/doc.pdf"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteMedia_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Archivo eliminado correctamente");

        when(mediaService.deleteMedia(1)).thenReturn(response);

        mockMvc.perform(delete("/media/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Archivo eliminado correctamente"));

        verify(mediaService).deleteMedia(1);
    }

    @Test
    void uploadMedia_deberiaResponder403_siNoAutenticado() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest(
                "imagen.png", "image", "desc", 3, "/files/imagen.png"
        );

        mockMvc.perform(post("/media/upload")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadMedia_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "originalFilename": "",
                  "mediaKind": "image",
                  "description": "desc",
                  "chapterId": 3,
                  "storagePath": "/files/imagen.png"
                }
                """;

        mockMvc.perform(post("/media/upload")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}