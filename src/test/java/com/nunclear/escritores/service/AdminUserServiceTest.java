package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.UpdateUserAccessLevelRequest;
import com.nunclear.escritores.dto.request.UpdateUserAccountStateRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.UserChangeHistory;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.UserChangeHistoryRepository;
import com.nunclear.escritores.repository.UserSessionRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserChangeHistoryRepository userChangeHistoryRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateAccessLevel_deberiaActualizarYGuardarHistorial() {
        Integer targetUserId = 10;
        Integer adminId = 99;

        AppUser targetUser = mock(AppUser.class);
        AppUser adminUser = mock(AppUser.class);
        AppUser savedUser = mock(AppUser.class);

        when(appUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(targetUser.getDeletedAt()).thenReturn(null);
        when(targetUser.getAccessLevel()).thenReturn(AccessLevel.user);

        when(adminUser.getId()).thenReturn(adminId);

        when(savedUser.getId()).thenReturn(targetUserId);
        when(savedUser.getAccessLevel()).thenReturn(AccessLevel.admin);
        when(savedUser.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 10, 0));

        when(appUserRepository.save(targetUser)).thenReturn(savedUser);

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(adminId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        UpdateUserAccessLevelRequest request = new UpdateUserAccessLevelRequest("admin");

        AdminUserAccessLevelResponse response = adminUserService.updateAccessLevel(targetUserId, request);

        assertNotNull(response);
        assertEquals(targetUserId, response.id());
        assertEquals("admin", response.accessLevel());
        assertEquals(LocalDateTime.of(2026, 4, 22, 10, 0), response.updatedAt());

        verify(targetUser).setAccessLevel(AccessLevel.admin);
        verify(appUserRepository).save(targetUser);

        ArgumentCaptor<UserChangeHistory> historyCaptor = ArgumentCaptor.forClass(UserChangeHistory.class);
        verify(userChangeHistoryRepository).save(historyCaptor.capture());

        UserChangeHistory history = historyCaptor.getValue();
        assertEquals(targetUserId, history.getUserId());
        assertEquals("accessLevel", history.getChangedField());
        assertEquals("user", history.getOldValue());
        assertEquals("admin", history.getNewValue());
        assertEquals(adminId, history.getChangedByUserId());
        assertNotNull(history.getChangedAt());
    }

    @Test
    void updateAccessLevel_deberiaLanzarBadRequest_siYaTieneEseRol() {
        Integer targetUserId = 10;
        Integer adminId = 99;

        AppUser targetUser = mock(AppUser.class);
        AppUser adminUser = mock(AppUser.class);

        when(appUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(targetUser.getDeletedAt()).thenReturn(null);
        when(targetUser.getAccessLevel()).thenReturn(AccessLevel.admin);

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(adminId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        UpdateUserAccessLevelRequest request = new UpdateUserAccessLevelRequest("admin");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> adminUserService.updateAccessLevel(targetUserId, request)
        );

        assertEquals("El usuario ya tiene ese accessLevel", ex.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(userChangeHistoryRepository, never()).save(any());
    }

    @Test
    void updateAccessLevel_deberiaLanzarBadRequest_siAccessLevelEsInvalido() {
        Integer targetUserId = 10;
        Integer adminId = 99;

        AppUser targetUser = mock(AppUser.class);
        AppUser adminUser = mock(AppUser.class);

        when(appUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(targetUser.getDeletedAt()).thenReturn(null);

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(adminId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        UpdateUserAccessLevelRequest request = new UpdateUserAccessLevelRequest("superadmin");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> adminUserService.updateAccessLevel(targetUserId, request)
        );

        assertEquals("accessLevel inválido", ex.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(userChangeHistoryRepository, never()).save(any());
    }

    @Test
    void updateAccountState_deberiaActualizarYRevocarSesiones_siEsBanned() {
        Integer targetUserId = 10;
        Integer adminId = 99;

        AppUser targetUser = mock(AppUser.class);
        AppUser adminUser = mock(AppUser.class);
        AppUser savedUser = mock(AppUser.class);

        Object session1 = mock(Object.class, invocation -> null);
        Object session2 = mock(Object.class, invocation -> null);

        when(appUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(targetUser.getDeletedAt()).thenReturn(null);
        when(targetUser.getAccountState()).thenReturn(AccountState.active);
        when(targetUser.getId()).thenReturn(targetUserId);

        when(adminUser.getId()).thenReturn(adminId);

        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(targetUserId))
                .thenReturn(List.of(
                        (com.nunclear.escritores.entity.UserSession) session1,
                        (com.nunclear.escritores.entity.UserSession) session2
                ));

        when(appUserRepository.save(targetUser)).thenReturn(savedUser);
        when(savedUser.getId()).thenReturn(targetUserId);
        when(savedUser.getAccountState()).thenReturn(AccountState.banned);
        when(savedUser.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 11, 0));

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(adminId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        UpdateUserAccountStateRequest request = new UpdateUserAccountStateRequest("banned");

        AdminUserAccountStateResponse response = adminUserService.updateAccountState(targetUserId, request);

        assertNotNull(response);
        assertEquals(targetUserId, response.id());
        assertEquals("banned", response.accountState());
        assertEquals(LocalDateTime.of(2026, 4, 22, 11, 0), response.updatedAt());

        verify(targetUser).setAccountState(AccountState.banned);
        verify(userSessionRepository).findByUserIdAndRevokedAtIsNull(targetUserId);
        verify(userSessionRepository, times(2)).save(any());

        ArgumentCaptor<UserChangeHistory> historyCaptor = ArgumentCaptor.forClass(UserChangeHistory.class);
        verify(userChangeHistoryRepository).save(historyCaptor.capture());

        UserChangeHistory history = historyCaptor.getValue();
        assertEquals("accountState", history.getChangedField());
        assertEquals("active", history.getOldValue());
        assertEquals("banned", history.getNewValue());
        assertEquals(adminId, history.getChangedByUserId());
    }

    @Test
    void updateAccountState_noDebeRevocarSesiones_siNoEsBanned() {
        Integer targetUserId = 10;
        Integer adminId = 99;

        AppUser targetUser = mock(AppUser.class);
        AppUser adminUser = mock(AppUser.class);
        AppUser savedUser = mock(AppUser.class);

        when(appUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(targetUser.getDeletedAt()).thenReturn(null);
        when(targetUser.getAccountState()).thenReturn(AccountState.pending_verification);

        when(adminUser.getId()).thenReturn(adminId);

        when(savedUser.getId()).thenReturn(targetUserId);
        when(savedUser.getAccountState()).thenReturn(AccountState.active);
        when(savedUser.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        when(appUserRepository.save(targetUser)).thenReturn(savedUser);

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(adminId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        UpdateUserAccountStateRequest request = new UpdateUserAccountStateRequest("active");

        AdminUserAccountStateResponse response = adminUserService.updateAccountState(targetUserId, request);

        assertEquals("active", response.accountState());
        verify(userSessionRepository, never()).findByUserIdAndRevokedAtIsNull(anyInt());
    }

    @Test
    void updateAccountState_deberiaLanzarBadRequest_siEstadoInvalido() {
        Integer targetUserId = 10;
        Integer adminId = 99;

        AppUser targetUser = mock(AppUser.class);
        AppUser adminUser = mock(AppUser.class);

        when(appUserRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(targetUser.getDeletedAt()).thenReturn(null);

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(adminId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        UpdateUserAccountStateRequest request = new UpdateUserAccountStateRequest("estado-raro");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> adminUserService.updateAccountState(targetUserId, request)
        );

        assertEquals("accountState inválido", ex.getMessage());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void getUserHistory_deberiaRetornarEventosOrdenados() {
        Integer userId = 10;

        AppUser user = mock(AppUser.class);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(null);

        UserChangeHistory event1 = new UserChangeHistory();
        event1.setChangedField("accessLevel");
        event1.setOldValue("user");
        event1.setNewValue("admin");
        event1.setChangedAt(LocalDateTime.of(2026, 4, 22, 9, 0));
        event1.setChangedByUserId(99);

        UserChangeHistory event2 = new UserChangeHistory();
        event2.setChangedField("accountState");
        event2.setOldValue("active");
        event2.setNewValue("banned");
        event2.setChangedAt(LocalDateTime.of(2026, 4, 22, 8, 0));
        event2.setChangedByUserId(99);

        Page<UserChangeHistory> page = new PageImpl<>(List.of(event1, event2));

        when(userChangeHistoryRepository.findByUserIdOrderByChangedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(page);

        UserHistoryResponse response = adminUserService.getUserHistory(userId);

        assertNotNull(response);
        assertEquals(2, response.events().size());

        UserHistoryEventResponse first = response.events().get(0);
        assertEquals("accessLevel", first.changedField());
        assertEquals("user", first.oldValue());
        assertEquals("admin", first.newValue());
        assertEquals(99, first.changedByUserId());
    }

    @Test
    void listUsersByRole_deberiaRetornarPageResponse() {
        AppUser user1 = mock(AppUser.class);
        AppUser user2 = mock(AppUser.class);

        when(user1.getId()).thenReturn(1);
        when(user1.getLoginName()).thenReturn("juan");
        when(user1.getAccessLevel()).thenReturn(AccessLevel.admin);

        when(user2.getId()).thenReturn(2);
        when(user2.getLoginName()).thenReturn("ana");
        when(user2.getAccessLevel()).thenReturn(AccessLevel.admin);

        Page<AppUser> page = new PageImpl<>(
                List.of(user1, user2),
                PageRequest.of(0, 20),
                2
        );

        when(appUserRepository.findByAccessLevelAndDeletedAtIsNull(eq(AccessLevel.admin), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AdminUserByRoleItemResponse> response =
                adminUserService.listUsersByRole("admin", 0, 20, "loginName,asc");

        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());

        assertEquals("juan", response.content().get(0).loginName());
        assertEquals("admin", response.content().get(0).accessLevel());
    }

    @Test
    void listUsersByState_deberiaRetornarPageResponse() {
        AppUser user1 = mock(AppUser.class);

        when(user1.getId()).thenReturn(1);
        when(user1.getLoginName()).thenReturn("pedro");
        when(user1.getAccountState()).thenReturn(AccountState.active);

        Page<AppUser> page = new PageImpl<>(
                List.of(user1),
                PageRequest.of(0, 20),
                1
        );

        when(appUserRepository.findByAccountStateAndDeletedAtIsNull(eq(AccountState.active), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AdminUserByStateItemResponse> response =
                adminUserService.listUsersByState("active", 0, 20, null);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("pedro", response.content().get(0).loginName());
        assertEquals("active", response.content().get(0).accountState());
    }

    @Test
    void deberiaLanzarResourceNotFound_siUsuarioObjetivoNoExiste() {
        when(appUserRepository.findById(123)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> adminUserService.getUserHistory(123)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void deberiaLanzarResourceNotFound_siUsuarioObjetivoEstaEliminado() {
        AppUser user = mock(AppUser.class);
        when(appUserRepository.findById(123)).thenReturn(Optional.of(user));
        when(user.getDeletedAt()).thenReturn(LocalDateTime.now());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> adminUserService.getUserHistory(123)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        AppUser targetUser = mock(AppUser.class);
        when(appUserRepository.findById(10)).thenReturn(Optional.of(targetUser));
        when(targetUser.getDeletedAt()).thenReturn(null);

        UpdateUserAccessLevelRequest request = new UpdateUserAccessLevelRequest("admin");

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> adminUserService.updateAccessLevel(10, request)
        );

        assertEquals("No autenticado", ex.getMessage());
    }
}