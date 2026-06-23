package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.ChapterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChapterController.class)
class ChapterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ChapterService chapterService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createChapter_deberiaResponder200() throws Exception {
        CreateChapterRequest request = new CreateChapterRequest(
                10, null, "Capítulo 1", "Sub", "contenido", null, "draft", 1
        );

        CreateChapterResponse response = new CreateChapterResponse(
                1, 10, "Capítulo 1", "draft", 1, 5
        );

        when(chapterService.createChapter(eq(request))).thenReturn(response);

        mockMvc.perform(post("/chapters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.title").value("Capítulo 1"))
                .andExpect(jsonPath("$.publicationState").value("draft"));
    }

    @Test
    void getChapterById_deberiaResponder200() throws Exception {
        ChapterDetailResponse response = new ChapterDetailResponse(
                1, 10, "Capítulo 1", "contenido", "published", 123
        );

        when(chapterService.getChapterById(1)).thenReturn(response);

        mockMvc.perform(get("/chapters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.storyId").value(10))
                .andExpect(jsonPath("$.title").value("Capítulo 1"));
    }

    @Test
    void getChaptersByStory_deberiaResponder200() throws Exception {
        PageResponse<ChapterListItemResponse> response = new PageResponse<>(
                List.of(new ChapterListItemResponse(1, "Capítulo 1", 1, "published", null)),
                0, 20, 1, 1
        );

        when(chapterService.getChaptersByStory(10, false, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/chapters/story/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Capítulo 1"))
                .andExpect(jsonPath("$.content[0].positionIndex").value(1));
    }

    @Test
    void getPublishedChaptersByStory_deberiaResponder200() throws Exception {
        PageResponse<ChapterListItemResponse> response = new PageResponse<>(
                List.of(new ChapterListItemResponse(1, "Publicado", 1, "published", null)),
                0, 20, 1, 1
        );

        when(chapterService.getPublishedChaptersByStory(10, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/chapters/story/10/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicationState").value("published"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyDrafts_deberiaResponder200() throws Exception {
        PageResponse<ChapterListItemResponse> response = new PageResponse<>(
                List.of(new ChapterListItemResponse(1, "Draft", 1, "draft", null)),
                0, 20, 1, 1
        );

        when(chapterService.getMyDrafts(null, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/chapters/me/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Draft"))
                .andExpect(jsonPath("$.content[0].publicationState").value("draft"));
    }

    @Test
    void searchChapters_deberiaResponder200() throws Exception {
        PageResponse<ChapterSearchItemResponse> response = new PageResponse<>(
                List.of(new ChapterSearchItemResponse(1, "Capítulo encontrado", "excerpt")),
                0, 20, 1, 1
        );

        when(chapterService.searchChapters("aventura", 10, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/chapters/search")
                        .param("q", "aventura")
                        .param("storyId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Capítulo encontrado"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateChapter_deberiaResponder200() throws Exception {
        UpdateChapterRequest request = new UpdateChapterRequest(
                "Nuevo título", "Sub", "contenido", 5, 2
        );

        UpdateChapterResponse response = new UpdateChapterResponse(
                1, "Nuevo título", LocalDateTime.of(2026, 4, 22, 12, 0)
        );

        when(chapterService.updateChapter(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/chapters/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nuevo título"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void publishChapter_deberiaResponder200() throws Exception {
        ChapterPublicationStateResponse response = new ChapterPublicationStateResponse(1, "published");

        when(chapterService.publishChapter(1)).thenReturn(response);

        mockMvc.perform(post("/chapters/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationState").value("published"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unpublishChapter_deberiaResponder200() throws Exception {
        ChapterPublicationStateResponse response = new ChapterPublicationStateResponse(1, "draft");

        when(chapterService.unpublishChapter(1)).thenReturn(response);

        mockMvc.perform(post("/chapters/1/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationState").value("draft"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void archiveChapter_deberiaResponder200() throws Exception {
        ChapterArchiveResponse response = new ChapterArchiveResponse(
                1, LocalDateTime.of(2026, 4, 22, 13, 0)
        );

        when(chapterService.archiveChapter(1)).thenReturn(response);

        mockMvc.perform(post("/chapters/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void reorderChapters_deberiaResponder200() throws Exception {
        ReorderChaptersRequest request = new ReorderChaptersRequest(
                10,
                List.of(
                        new ReorderChapterItemRequest(1, 2),
                        new ReorderChapterItemRequest(2, 1)
                )
        );

        MessageResponse response = new MessageResponse("Capítulos reordenados correctamente");

        when(chapterService.reorderChapters(eq(request))).thenReturn(response);

        mockMvc.perform(post("/chapters/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Capítulos reordenados correctamente"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void moveChapter_deberiaResponder200() throws Exception {
        MoveChapterRequest request = new MoveChapterRequest(7, 4);
        MoveChapterResponse response = new MoveChapterResponse(1, 7, 4);

        when(chapterService.moveChapter(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(post("/chapters/1/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.volumeId").value(7))
                .andExpect(jsonPath("$.positionIndex").value(4));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteChapter_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Capítulo eliminado correctamente");

        when(chapterService.deleteChapter(1)).thenReturn(response);

        mockMvc.perform(delete("/chapters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Capítulo eliminado correctamente"));

        verify(chapterService).deleteChapter(1);
    }

    @Test
    void createChapter_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateChapterRequest request = new CreateChapterRequest(
                10, null, "Capítulo 1", "Sub", "contenido", null, "draft", 1
        );

        mockMvc.perform(post("/chapters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createChapter_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "volumeId": null,
                  "title": "",
                  "subtitle": "Sub",
                  "content": "contenido",
                  "publishedOn": null,
                  "publicationState": "draft",
                  "positionIndex": 1
                }
                """;

        mockMvc.perform(post("/chapters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}