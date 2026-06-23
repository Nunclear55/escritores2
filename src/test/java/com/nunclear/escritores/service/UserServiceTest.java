package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.UserFollow;
import com.nunclear.escritores.entity.UserSession;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ConflictException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.repository.UserFollowRepository;
import com.nunclear.escritores.repository.UserSessionRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private UserFollowRepository userFollowRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserById_deberiaRetornarPerfil() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.active);
        when(user.getId()).thenReturn(1);
        when(user.getLoginName()).thenReturn("juan");
        when(user.getDisplayName()).thenReturn("Juan");
        when(user.getBioText()).thenReturn("Bio");
        when(user.getAvatarUrl()).thenReturn("https://img.test/a.jpg");
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);
        when(user.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 10, 0));

        UserProfileResponse response = userService.getUserById(1);

        assertEquals(1, response.id());
        assertEquals("juan", response.loginName());
        assertEquals("Juan", response.displayName());
        assertEquals("user", response.accessLevel());
    }

    @Test
    void getUserById_deberiaLanzarNotFound_siUsuarioEliminado() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(LocalDateTime.now());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(1)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getUserById_deberiaLanzarNotFound_siUsuarioBaneado() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.banned);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(1)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getMyProfile_deberiaRetornarPerfilActual() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getLoginName()).thenReturn("juan");
        when(user.getEmailAddress()).thenReturn("juan@test.com");
        when(user.getDisplayName()).thenReturn("Juan");
        when(user.getBioText()).thenReturn("Bio");
        when(user.getAvatarUrl()).thenReturn("https://img.test/a.jpg");
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);
        when(user.getAccountState()).thenReturn(AccountState.active);

        CurrentUserResponse response = userService.getMyProfile();

        assertEquals(1, response.id());
        assertEquals("juan@test.com", response.emailAddress());
        assertEquals("active", response.accountState());
    }

    @Test
    void listUsers_deberiaRetornarPagina() {
        AppUser user = mock(AppUser.class);
        Page<AppUser> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);

        when(appUserRepository.findAllActive(any(Pageable.class))).thenReturn(page);
        when(user.getId()).thenReturn(1);
        when(user.getLoginName()).thenReturn("juan");
        when(user.getDisplayName()).thenReturn("Juan");
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);
        when(user.getAccountState()).thenReturn(AccountState.active);

        PageResponse<UserListItemResponse> response = userService.listUsers(0, 20, "createdAt,desc");

        assertEquals(1, response.content().size());
        assertEquals("juan", response.content().get(0).loginName());
    }

    @Test
    void searchUsers_deberiaRetornarPagina() {
        AppUser user = mock(AppUser.class);
        Page<AppUser> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);

        when(appUserRepository.searchUsers(eq("juan"), any(Pageable.class))).thenReturn(page);
        when(user.getId()).thenReturn(1);
        when(user.getLoginName()).thenReturn("juan");
        when(user.getDisplayName()).thenReturn("Juan");
        when(user.getAvatarUrl()).thenReturn("https://img.test/a.jpg");

        PageResponse<UserSearchItemResponse> response = userService.searchUsers("juan", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Juan", response.content().get(0).displayName());
    }

    @Test
    void updateMyProfile_deberiaActualizarPerfil() {
        AppUser user = mock(AppUser.class);
        AppUser saved = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        when(appUserRepository.save(user)).thenReturn(saved);
        when(saved.getId()).thenReturn(1);
        when(saved.getDisplayName()).thenReturn("Juan Actualizado");
        when(saved.getBioText()).thenReturn("Nueva bio");
        when(saved.getAvatarUrl()).thenReturn("https://img.test/new.jpg");
        when(saved.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        UpdateMyProfileResponse response = userService.updateMyProfile(
                new UpdateMyProfileRequest("Juan Actualizado", "Nueva bio", "https://img.test/new.jpg")
        );

        assertEquals(1, response.id());
        assertEquals("Juan Actualizado", response.displayName());

        verify(user).setDisplayName("Juan Actualizado");
        verify(user).setBioText("Nueva bio");
        verify(user).setAvatarUrl("https://img.test/new.jpg");
    }

    @Test
    void changeAvatar_deberiaActualizarAvatar() {
        AppUser user = mock(AppUser.class);
        AppUser saved = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        when(appUserRepository.save(user)).thenReturn(saved);
        when(saved.getAvatarUrl()).thenReturn("https://img.test/new.jpg");
        when(saved.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        AvatarResponse response = userService.changeAvatar(
                new ChangeAvatarRequest("https://img.test/new.jpg")
        );

        assertEquals("https://img.test/new.jpg", response.avatarUrl());
        verify(user).setAvatarUrl("https://img.test/new.jpg");
    }

    @Test
    void changePassword_deberiaActualizarPasswordYRevocarSesiones() {
        AppUser user = mock(AppUser.class);
        UserSession session1 = mock(UserSession.class);
        UserSession session2 = mock(UserSession.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1);
        when(user.getPasswordHash()).thenReturn("oldHash");

        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass123", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");

        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(1)).thenReturn(List.of(session1, session2));

        MessageResponse response = userService.changePassword(
                new ChangePasswordRequest("oldPass", "newPass123")
        );

        assertEquals("Contraseña actualizada correctamente", response.message());
        verify(user).setPasswordHash("newHash");
        verify(appUserRepository).save(user);
        verify(session1).setRevokedAt(any(LocalDateTime.class));
        verify(session2).setRevokedAt(any(LocalDateTime.class));
        verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void changePassword_deberiaLanzarBadRequest_siOldPasswordIncorrecta() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("oldHash");
        when(passwordEncoder.matches("mala", "oldHash")).thenReturn(false);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userService.changePassword(new ChangePasswordRequest("mala", "newPass123"))
        );

        assertEquals("La contraseña actual no es correcta", ex.getMessage());
    }

    @Test
    void changePassword_deberiaLanzarBadRequest_siNuevaEsIgual() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("oldHash");
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userService.changePassword(new ChangePasswordRequest("oldPass", "oldPass"))
        );

        assertEquals("La nueva contraseña no puede ser igual a la anterior", ex.getMessage());
    }

    @Test
    void changeEmail_deberiaActualizarPendingEmail() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        when(appUserRepository.existsByEmailAddressIgnoreCase("nuevo@test.com")).thenReturn(false);
        when(appUserRepository.existsByPendingEmailAddressIgnoreCase("nuevo@test.com")).thenReturn(false);
        when(user.getPendingEmailAddress()).thenReturn("nuevo@test.com");

        ChangeEmailResponse response = userService.changeEmail(
                new ChangeEmailRequest("nuevo@test.com", "secret")
        );

        assertEquals("Cambio de correo solicitado", response.message());
        assertEquals("nuevo@test.com", response.pendingEmailAddress());

        verify(user).setPendingEmailAddress("nuevo@test.com");
        verify(user).setEmailChangeRequestedAt(any(LocalDateTime.class));
        verify(appUserRepository).save(user);
    }

    @Test
    void changeEmail_deberiaLanzarBadRequest_siPasswordIncorrecta() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userService.changeEmail(new ChangeEmailRequest("nuevo@test.com", "bad"))
        );

        assertEquals("La contraseña no es correcta", ex.getMessage());
    }

    @Test
    void deactivateMyAccount_deberiaBanearYRevocarSesiones() {
        AppUser user = mock(AppUser.class);
        UserSession session = mock(UserSession.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(1)).thenReturn(List.of(session));

        MessageResponse response = userService.deactivateMyAccount();

        assertEquals("Cuenta desactivada correctamente", response.message());
        verify(user).setAccountState(AccountState.banned);
        verify(user).setDeletedAt(any(LocalDateTime.class));
        verify(appUserRepository).save(user);
        verify(session).setRevokedAt(any(LocalDateTime.class));
        verify(userSessionRepository).save(session);
    }

    @Test
    void getPublicAuthorProfile_deberiaRetornarPerfilPublico() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.active);
        when(user.getId()).thenReturn(2);
        when(user.getDisplayName()).thenReturn("Ana");
        when(user.getBioText()).thenReturn("Bio");
        when(user.getAvatarUrl()).thenReturn("https://img.test/a.jpg");

        when(userFollowRepository.countByFollowedUserId(2)).thenReturn(15L);
        when(storyRepository.countByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
                2, "public", "published"
        )).thenReturn(4L);

        PublicAuthorProfileResponse response = userService.getPublicAuthorProfile(2);

        assertEquals(2, response.id());
        assertEquals("Ana", response.displayName());
        assertEquals(15L, response.followersCount());
        assertEquals(4L, response.storiesCount());
    }

    @Test
    void getPublicStoriesByAuthor_deberiaRetornarPagina() {
        AppUser user = mock(AppUser.class);
        Story story = mock(Story.class);
        Page<Story> page = new PageImpl<>(List.of(story), PageRequest.of(0, 20), 1);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.active);

        when(storyRepository.findByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
                eq(2), eq("public"), eq("published"), any(Pageable.class)
        )).thenReturn(page);

        when(story.getId()).thenReturn(10);
        when(story.getTitle()).thenReturn("Historia");
        when(story.getSlugText()).thenReturn("historia");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getVisibilityState()).thenReturn("public");

        PageResponse<UserStoryItemResponse> response = userService.getPublicStoriesByAuthor(2, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Historia", response.content().get(0).title());
    }

    @Test
    void getAuthenticatedUser_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> userService.getMyProfile()
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    // ==================== ADDITIONAL COVERAGE TESTS ====================

    @Test
    void getUserById_deberiaLanzarNotFound_siUsuarioSuspendido() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.suspended);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(1)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getUserById_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(999)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getMyProfile_deberiaLanzarUnauthorized_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticated(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getMyProfile()
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void listUsers_deberiaRetornarListaVacia() {
        Page<AppUser> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(appUserRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        PageResponse<UserListItemResponse> response = userService.listUsers(0, 20, "createdAt,desc");

        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
    }

    @Test
    void listUsers_deberiaOrdearCorrectamente() {
        AppUser user1 = mock(AppUser.class);
        AppUser user2 = mock(AppUser.class);
        Page<AppUser> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);

        when(appUserRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        when(user1.getId()).thenReturn(1);
        when(user1.getLoginName()).thenReturn("alice");
        when(user1.getDisplayName()).thenReturn("Alice");
        when(user1.getAccessLevel()).thenReturn(AccessLevel.user);
        when(user1.getAccountState()).thenReturn(AccountState.active);

        when(user2.getId()).thenReturn(2);
        when(user2.getLoginName()).thenReturn("bob");
        when(user2.getDisplayName()).thenReturn("Bob");
        when(user2.getAccessLevel()).thenReturn(AccessLevel.user);
        when(user2.getAccountState()).thenReturn(AccountState.active);

        PageResponse<UserListItemResponse> response = userService.listUsers(0, 20, "loginName,asc");

        assertEquals(2, response.content().size());
        assertEquals("alice", response.content().get(0).loginName());
        assertEquals("bob", response.content().get(1).loginName());
    }

    @Test
    void searchUsers_deberiaRetornarListaVacia() {
        Page<AppUser> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(appUserRepository.searchUsers(eq("noexiste"), any(Pageable.class))).thenReturn(page);

        PageResponse<UserSearchItemResponse> response = userService.searchUsers("noexiste", 0, 20, null);

        assertEquals(0, response.content().size());
    }

    @Test
    void searchUsers_deberiaMultipleResultados() {
        AppUser user1 = mock(AppUser.class);
        AppUser user2 = mock(AppUser.class);
        Page<AppUser> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);

        when(appUserRepository.searchUsers(eq("ana"), any(Pageable.class))).thenReturn(page);

        when(user1.getId()).thenReturn(1);
        when(user1.getLoginName()).thenReturn("ana");
        when(user1.getDisplayName()).thenReturn("Ana");
        when(user1.getAvatarUrl()).thenReturn("https://img.test/a.jpg");

        when(user2.getId()).thenReturn(2);
        when(user2.getLoginName()).thenReturn("anabel");
        when(user2.getDisplayName()).thenReturn("Anabel");
        when(user2.getAvatarUrl()).thenReturn("https://img.test/b.jpg");

        PageResponse<UserSearchItemResponse> response = userService.searchUsers("ana", 0, 20, "loginName,asc");

        assertEquals(2, response.content().size());
        assertEquals("ana", response.content().get(0).loginName());
    }

    @Test
    void updateMyProfile_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> userService.updateMyProfile(
                        new UpdateMyProfileRequest("New Name", "New Bio", "https://url.jpg")
                )
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void updateMyProfile_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticated(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateMyProfile(
                        new UpdateMyProfileRequest("New Name", "New Bio", "https://url.jpg")
                )
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void updateMyProfile_deberiaGuardarConNulosOpcionales() {
        AppUser user = mock(AppUser.class);
        AppUser saved = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        when(appUserRepository.save(user)).thenReturn(saved);
        when(saved.getId()).thenReturn(1);
        when(saved.getDisplayName()).thenReturn("Name");
        when(saved.getBioText()).thenReturn(null);
        when(saved.getAvatarUrl()).thenReturn(null);
        when(saved.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        UpdateMyProfileResponse response = userService.updateMyProfile(
                new UpdateMyProfileRequest("Name", null, null)
        );

        assertEquals(1, response.id());
        assertEquals("Name", response.displayName());
        verify(user).setDisplayName("Name");
    }

    @Test
    void changeAvatar_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> userService.changeAvatar(new ChangeAvatarRequest("https://url.jpg"))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void changeAvatar_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticated(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.changeAvatar(new ChangeAvatarRequest("https://url.jpg"))
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void changePassword_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "new"))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void changePassword_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticated(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.changePassword(new ChangePasswordRequest("old", "new"))
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void changePassword_deberiaNoRevocarSesionesSiNoHay() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1);
        when(user.getPasswordHash()).thenReturn("oldHash");

        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");

        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(1)).thenReturn(List.of());

        MessageResponse response = userService.changePassword(
                new ChangePasswordRequest("oldPass", "newPass123")
        );

        assertEquals("Contraseña actualizada correctamente", response.message());
        verify(appUserRepository).save(user);
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void changeEmail_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> userService.changeEmail(new ChangeEmailRequest("new@test.com", "pass"))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void changeEmail_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticated(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.changeEmail(new ChangeEmailRequest("new@test.com", "pass"))
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void changeEmail_deberiaLanzarConflict_siEmailYaUsado() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        when(appUserRepository.existsByEmailAddressIgnoreCase("used@test.com")).thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> userService.changeEmail(new ChangeEmailRequest("used@test.com", "pass"))
        );

        assertEquals("El correo ya está en uso", ex.getMessage());
    }

    @Test
    void changeEmail_deberiaLanzarConflict_siPendingEmailYaUsado() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        when(appUserRepository.existsByEmailAddressIgnoreCase("pending@test.com")).thenReturn(false);
        when(appUserRepository.existsByPendingEmailAddressIgnoreCase("pending@test.com")).thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> userService.changeEmail(new ChangeEmailRequest("pending@test.com", "pass"))
        );

        assertEquals("El correo ya está en uso", ex.getMessage());
    }

    @Test
    void deactivateMyAccount_deberiaLanzarUnauthorized_siNoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> userService.deactivateMyAccount()
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    @Test
    void deactivateMyAccount_deberiaLanzarNotFound_siUsuarioNoEncontrado() {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(999);
        mockAuthenticated(principal);
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.deactivateMyAccount()
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void deactivateMyAccount_deberiaSinSesiones() {
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(1)).thenReturn(List.of());

        MessageResponse response = userService.deactivateMyAccount();

        assertEquals("Cuenta desactivada correctamente", response.message());
        verify(user).setAccountState(AccountState.banned);
        verify(user).setDeletedAt(any(LocalDateTime.class));
        verify(appUserRepository).save(user);
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void getPublicAuthorProfile_deberiaLanzarNotFound_siUsuarioEliminado() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(LocalDateTime.now());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getPublicAuthorProfile(2)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getPublicAuthorProfile_deberiaLanzarNotFound_siUsuarioBaneado() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.banned);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getPublicAuthorProfile(2)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getPublicAuthorProfile_deberiaLanzarNotFound_siNoEncontrado() {
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getPublicAuthorProfile(999)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getPublicStoriesByAuthor_deberiaLanzarNotFound_siAutorNoEncontrado() {
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getPublicStoriesByAuthor(999, 0, 20, null)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getPublicStoriesByAuthor_deberiaLanzarNotFound_siAutorEliminado() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(LocalDateTime.now());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getPublicStoriesByAuthor(2, 0, 20, null)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getPublicStoriesByAuthor_deberiaLanzarNotFound_siAutorBaneado() {
        AppUser user = mock(AppUser.class);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.banned);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getPublicStoriesByAuthor(2, 0, 20, null)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void getPublicStoriesByAuthor_deberiaRetornarPaginaVacia() {
        AppUser user = mock(AppUser.class);
        Page<Story> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.active);

        when(storyRepository.findByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
                eq(2), eq("public"), eq("published"), any(Pageable.class)
        )).thenReturn(page);

        PageResponse<UserStoryItemResponse> response = userService.getPublicStoriesByAuthor(2, 0, 20, null);

        assertEquals(0, response.content().size());
    }

    @Test
    void getPublicStoriesByAuthor_deberiaRetornarMultipleStories() {
        AppUser user = mock(AppUser.class);
        Story story1 = mock(Story.class);
        Story story2 = mock(Story.class);
        Page<Story> page = new PageImpl<>(List.of(story1, story2), PageRequest.of(0, 20), 2);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getAccountState()).thenReturn(AccountState.active);

        when(storyRepository.findByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
                eq(2), eq("public"), eq("published"), any(Pageable.class)
        )).thenReturn(page);

        when(story1.getId()).thenReturn(10);
        when(story1.getTitle()).thenReturn("Historia 1");
        when(story1.getSlugText()).thenReturn("historia-1");
        when(story1.getPublicationState()).thenReturn("published");
        when(story1.getVisibilityState()).thenReturn("public");

        when(story2.getId()).thenReturn(11);
        when(story2.getTitle()).thenReturn("Historia 2");
        when(story2.getSlugText()).thenReturn("historia-2");
        when(story2.getPublicationState()).thenReturn("published");
        when(story2.getVisibilityState()).thenReturn("public");

        PageResponse<UserStoryItemResponse> response = userService.getPublicStoriesByAuthor(2, 0, 20, "createdAt,desc");

        assertEquals(2, response.content().size());
        assertEquals("Historia 1", response.content().get(0).title());
        assertEquals("Historia 2", response.content().get(1).title());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}
