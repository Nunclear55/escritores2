package com.nunclear.escritores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nunclear.escritores.config.SecurityConfig;
import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.security.JwtAuthenticationFilter;
import com.nunclear.escritores.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getUserById_deberiaResponder200() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1, "juan", "Juan", "Bio", "https://img.test/a.jpg", "user",
                LocalDateTime.of(2026, 4, 22, 10, 0)
        );

        when(userService.getUserById(1)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("juan"))
                .andExpect(jsonPath("$.displayName").value("Juan"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyProfile_deberiaResponder200() throws Exception {
        CurrentUserResponse response = new CurrentUserResponse(
                1, "juan", "juan@test.com", "Juan", "Bio", "https://img.test/a.jpg", "user", "active"
        );

        when(userService.getMyProfile()).thenReturn(response);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("juan@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_deberiaResponder200() throws Exception {
        PageResponse<UserListItemResponse> response = new PageResponse<>(
                List.of(new UserListItemResponse(1, "juan", "Juan", "user", "active")),
                0, 20, 1, 1
        );

        when(userService.listUsers(0, 20, "createdAt,desc")).thenReturn(response);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].loginName").value("juan"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchUsers_deberiaResponder200() throws Exception {
        PageResponse<UserSearchItemResponse> response = new PageResponse<>(
                List.of(new UserSearchItemResponse(1, "juan", "Juan", "https://img.test/a.jpg")),
                0, 20, 1, 1
        );

        when(userService.searchUsers("juan", 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/users/search").param("q", "juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].displayName").value("Juan"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateMyProfile_deberiaResponder200() throws Exception {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("Juan Nuevo", "Bio nueva", "https://img.test/b.jpg");
        UpdateMyProfileResponse response = new UpdateMyProfileResponse(
                1, "Juan Nuevo", "Bio nueva", "https://img.test/b.jpg",
                LocalDateTime.of(2026, 4, 22, 12, 0)
        );

        when(userService.updateMyProfile(eq(request))).thenReturn(response);

        mockMvc.perform(put("/users/me")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Juan Nuevo"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changeAvatar_deberiaResponder200() throws Exception {
        ChangeAvatarRequest request = new ChangeAvatarRequest("https://img.test/new.jpg");
        AvatarResponse response = new AvatarResponse("https://img.test/new.jpg",
                LocalDateTime.of(2026, 4, 22, 12, 0));

        when(userService.changeAvatar(eq(request))).thenReturn(response);

        mockMvc.perform(patch("/users/me/avatar")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://img.test/new.jpg"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changePassword_deberiaResponder200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123");
        MessageResponse response = new MessageResponse("Contraseña actualizada correctamente");

        when(userService.changePassword(eq(request))).thenReturn(response);

        mockMvc.perform(post("/users/me/change-password")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changeEmail_deberiaResponder200() throws Exception {
        ChangeEmailRequest request = new ChangeEmailRequest("nuevo@test.com", "secret");
        ChangeEmailResponse response = new ChangeEmailResponse("Cambio de correo solicitado", "nuevo@test.com");

        when(userService.changeEmail(eq(request))).thenReturn(response);

        mockMvc.perform(post("/users/me/change-email")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingEmailAddress").value("nuevo@test.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deactivateMyAccount_deberiaResponder200() throws Exception {
        MessageResponse response = new MessageResponse("Cuenta desactivada correctamente");

        when(userService.deactivateMyAccount()).thenReturn(response);

        mockMvc.perform(post("/users/me/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta desactivada correctamente"));
    }

    @Test
    void getPublicAuthorProfile_deberiaResponder200() throws Exception {
        PublicAuthorProfileResponse response = new PublicAuthorProfileResponse(
                2, "Ana", "Bio", "https://img.test/a.jpg", 15, 4
        );

        when(userService.getPublicAuthorProfile(2)).thenReturn(response);

        mockMvc.perform(get("/users/2/public-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ana"))
                .andExpect(jsonPath("$.followersCount").value(15));
    }

    @Test
    void getPublicStoriesByAuthor_deberiaResponder200() throws Exception {
        PageResponse<UserStoryItemResponse> response = new PageResponse<>(
                List.of(new UserStoryItemResponse(10, "Historia", "historia", "published", "public")),
                0, 20, 1, 1
        );

        when(userService.getPublicStoriesByAuthor(2, 0, 20, null)).thenReturn(response);

        mockMvc.perform(get("/users/2/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Historia"));
    }

    @Test
    void getMyProfile_deberiaResponder403_siNoAutenticado() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateMyProfile_deberiaResponder400_siBodyInvalido() throws Exception {
        String body = """
                {
                  "displayName": "",
                  "bioText": "Bio",
                  "avatarUrl": "https://img.test/a.jpg"
                }
                """;

        mockMvc.perform(put("/users/me")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}