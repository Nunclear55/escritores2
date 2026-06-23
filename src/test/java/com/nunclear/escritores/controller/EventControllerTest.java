package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.dto.request.CreateEventRequest;
import com.nunclear.escritores.dto.request.UpdateEventRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createEvent_deberiaResponder200() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                10, 3, "Batalla", "Desc", LocalDate.of(2026, 1, 1), 5,
                "combat", List.of("war"), List.of(11)
        );
        CreateEventResponse response = new CreateEventResponse(1, 10, 3, "Batalla");

        when(eventService.createEvent(eq(request))).thenReturn(response);

        mockMvc.perform(post("/events")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Batalla"));
    }

    @Test
    void getEventById_deberiaResponder200() throws Exception {
        EventDetailResponse response = new EventDetailResponse(1, "Batalla", "Desc", 5, "combat");

        when(eventService.getEventById(1)).thenReturn(response);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Batalla"));
    }

    @Test
    void getEventsByStory_deberiaResponder200() throws Exception {
        PageResponse<EventListItemResponse> response = new PageResponse<>(
                List.of(new EventListItemResponse(1, "Batalla", 3)),
                0, 20, 1, 1
        );

        when(eventService.getEventsByStory(10, "combat", 5, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/events/story/10")
                        .param("eventKind", "combat")
                        .param("importance", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Batalla"));
    }

    @Test
    void getEventsByChapter_deberiaResponder200() throws Exception {
        PageResponse<EventListItemResponse> response = new PageResponse<>(
                List.of(new EventListItemResponse(1, "Batalla", 3)),
                0, 20, 1, 1
        );

        when(eventService.getEventsByChapter(3, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/events/chapter/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Batalla"));
    }

    @Test
    void searchEvents_deberiaResponder200() throws Exception {
        PageResponse<EventListItemResponse> response = new PageResponse<>(
                List.of(new EventListItemResponse(1, "Batalla", 3)),
                0, 20, 1, 1
        );

        when(eventService.searchEvents("bat", "war", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/events/search")
                        .param("q", "bat")
                        .param("tag", "war"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Batalla"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateEvent_deberiaResponder200() throws Exception {
        UpdateEventRequest request = new UpdateEventRequest(
                "Batalla final", "Desc", LocalDate.of(2026, 1, 2), 9,
                "combat", List.of("final"), List.of(11)
        );
        UpdateEventResponse response = new UpdateEventResponse(1, "Batalla final");

        when(eventService.updateEvent(eq(1), eq(request))).thenReturn(response);

        mockMvc.perform(put("/events/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Batalla final"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteEvent_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Evento eliminado correctamente");

        when(eventService.deleteEvent(1)).thenReturn(response);

        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Evento eliminado correctamente"));

        verify(eventService).deleteEvent(1);
    }

    @Test
    void createEvent_deberiaResponder403_siNoAutenticado() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                10, null, "Batalla", null, null, null, null, null, null
        );

        mockMvc.perform(post("/events")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createEvent_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "storyId": 10,
                  "chapterId": null,
                  "title": "",
                  "description": "Desc",
                  "eventOn": "2026-01-01",
                  "importance": 5,
                  "eventKind": "combat",
                  "tagsJson": ["war"],
                  "linkedCharacterIds": [11]
                }
                """;

        mockMvc.perform(post("/events")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}