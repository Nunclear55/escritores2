package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateStoryRequest;
import com.nunclear.escritores.dto.request.DuplicateStoryRequest;
import com.nunclear.escritores.dto.request.UpdateStoryRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.StoryService;
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

@WebMvcTest(StoryController.class)
class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StoryService storyService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createStory_deberiaResponder201o200_segunTuConfigActual() throws Exception {
        CreateStoryResponse response = new CreateStoryResponse(
                1,
                10,
                "Historia",
                "historia",
                "public",
                "draft",
                LocalDateTime.of(2026, 4, 22, 10, 0)
        );

        CreateStoryRequest request = new CreateStoryRequest(
                "Historia",
                "Desc",
                "https://img.com/1.jpg",
                "public",
                "draft",
                true,
                true,
                null
        );

        when(storyService.createStory(eq(request))).thenReturn(response);

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Historia"))
                .andExpect(jsonPath("$.slugText").value("historia"));
    }

    @Test
    void getStoryById_deberiaResponder200() throws Exception {
        StoryDetailResponse response = new StoryDetailResponse(
                1,
                10,
                "Historia",
                "historia",
                "Desc",
                "public",
                "published"
        );

        when(storyService.getStoryById(1)).thenReturn(response);

        mockMvc.perform(get("/stories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Historia"))
                .andExpect(jsonPath("$.slugText").value("historia"));
    }

    @Test
    void getStoryBySlug_deberiaResponder200() throws Exception {
        StorySlugResponse response = new StorySlugResponse(1, "mi-slug", "Historia");

        when(storyService.getStoryBySlug("mi-slug")).thenReturn(response);

        mockMvc.perform(get("/stories/slug/mi-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.slugText").value("mi-slug"))
                .andExpect(jsonPath("$.title").value("Historia"));
    }

    @Test
    void listPublicStories_deberiaResponder200() throws Exception {
        PageResponse<StoryListItemResponse> response = new PageResponse<>(
                List.of(new StoryListItemResponse(1, "Historia", "historia", "public", "published")),
                0,
                20,
                1,
                1
        );

        when(storyService.listPublicStories(0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Historia"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchStories_deberiaResponder200() throws Exception {
        PageResponse<StoryListItemResponse> response = new PageResponse<>(
                List.of(new StoryListItemResponse(1, "Historia", "historia", "public", "published")),
                0,
                20,
                1,
                1
        );

        when(storyService.searchStories("aventura", null, null, 0, 20, null))
                .thenReturn(response);

        mockMvc.perform(get("/stories/search")
                        .param("q", "aventura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slugText").value("historia"));
    }

    @Test
    void getStoriesByUser_deberiaResponder200() throws Exception {
        PageResponse<UserStorySummaryResponse> response = new PageResponse<>(
                List.of(new UserStorySummaryResponse(1, 5, "Historia", "published")),
                0,
                20,
                1,
                1
        );

        when(storyService.getStoriesByUser(5, false, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/stories/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ownerUserId").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Historia"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyDrafts_deberiaResponder200() throws Exception {
        PageResponse<UserStorySummaryResponse> response = new PageResponse<>(
                List.of(new UserStorySummaryResponse(1, 5, "Borrador", "draft")),
                0,
                20,
                1,
                1
        );

        when(storyService.getMyDrafts(0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/stories/me/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicationState").value("draft"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyArchived_deberiaResponder200() throws Exception {
        PageResponse<ArchivedStoryItemResponse> response = new PageResponse<>(
                List.of(new ArchivedStoryItemResponse(1, "Archivada", LocalDateTime.of(2026, 4, 22, 12, 0))),
                0,
                20,
                1,
                1
        );

        when(storyService.getMyArchived(0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/stories/me/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Archivada"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateStory_deberiaResponder200() throws Exception {
        UpdateStoryRequest request = new UpdateStoryRequest(
                "Nuevo título",
                "Nueva desc",
                "https://img.com/new.jpg",
                "private",
                false,
                false
        );

        UpdateStoryResponse response = new UpdateStoryResponse(
                1,
                "Nuevo título",
                LocalDateTime.of(2026, 4, 22, 13, 0)
        );

        when(storyService.updateStory(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/stories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nuevo título"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void publishStory_deberiaResponder200() throws Exception {
        StoryPublicationResponse response = new StoryPublicationResponse(
                1,
                "published",
                LocalDateTime.of(2026, 4, 22, 14, 0)
        );

        when(storyService.publishStory(1)).thenReturn(response);

        mockMvc.perform(post("/stories/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationState").value("published"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unpublishStory_deberiaResponder200() throws Exception {
        StoryPublicationResponse response = new StoryPublicationResponse(1, "draft", null);

        when(storyService.unpublishStory(1)).thenReturn(response);

        mockMvc.perform(post("/stories/1/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationState").value("draft"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void archiveStory_deberiaResponder200() throws Exception {
        StoryArchiveResponse response = new StoryArchiveResponse(
                1,
                LocalDateTime.of(2026, 4, 22, 15, 0)
        );

        when(storyService.archiveStory(1)).thenReturn(response);

        mockMvc.perform(post("/stories/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void restoreStory_deberiaResponder200() throws Exception {
        StoryArchiveResponse response = new StoryArchiveResponse(1, null);

        when(storyService.restoreStory(1)).thenReturn(response);

        mockMvc.perform(post("/stories/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void duplicateStory_deberiaResponder200() throws Exception {
        DuplicateStoryRequest request = new DuplicateStoryRequest("Copia");

        DuplicateStoryResponse response = new DuplicateStoryResponse(
                2,
                1,
                "Copia",
                "draft"
        );

        when(storyService.duplicateStory(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(post("/stories/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.sourceStoryId").value(1))
                .andExpect(jsonPath("$.publicationState").value("draft"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteStory_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Historia eliminada correctamente");

        when(storyService.deleteStory(1)).thenReturn(response);

        mockMvc.perform(delete("/stories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Historia eliminada correctamente"));

        verify(storyService).deleteStory(1);
    }

    @Test
    void createStory_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateStoryRequest request = new CreateStoryRequest(
                "Historia",
                "Desc",
                null,
                "public",
                "draft",
                true,
                true,
                null
        );

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createStory_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "title": "",
                  "description": "Desc",
                  "coverImageUrl": null,
                  "visibilityState": "public",
                  "publicationState": "draft",
                  "allowFeedback": true,
                  "allowScores": true,
                  "startedOn": null
                }
                """;

        mockMvc.perform(post("/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}