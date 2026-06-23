package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.EmailVerificationToken;
import com.nunclear.escritores.entity.PasswordResetToken;
import com.nunclear.escritores.entity.UserSession;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ConflictException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.EmailVerificationTokenRepository;
import com.nunclear.escritores.repository.PasswordResetTokenRepository;
import com.nunclear.escritores.repository.UserSessionRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import com.nunclear.escritores.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessExpirationSeconds", 3600L);
        ReflectionTestUtils.setField(authService, "refreshExpirationSeconds", 604800L);
        SecurityContextHolder.clearContext();
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void should_register_successfully_when_all_data_is_valid() {
        RegisterRequest request = new RegisterRequest(
                "usuario1",
                "usuario1@test.com",
                "Usuario Uno",
                "Password123"
        );

        when(appUserRepository.existsByLoginNameIgnoreCase("usuario1")).thenReturn(false);
        when(appUserRepository.existsByEmailAddressIgnoreCase("usuario1@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-pass");

        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser u = invocation.getArgument(0);
            u.setId(1);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(1, response.id());
        assertEquals("usuario1", response.loginName());
        assertEquals("usuario1@test.com", response.emailAddress());
        assertEquals("Usuario Uno", response.displayName());
        assertEquals("user", response.accessLevel());
        assertEquals("pending_verification", response.accountState());

        verify(appUserRepository).save(any(AppUser.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(passwordEncoder).encode("Password123");
    }

    @Test
    void should_throw_conflict_when_login_name_already_exists() {
        RegisterRequest request = new RegisterRequest(
                "usuario1",
                "usuario1@test.com",
                "Usuario Uno",
                "Password123"
        );

        when(appUserRepository.existsByLoginNameIgnoreCase("usuario1")).thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> authService.register(request)
        );

        assertEquals("El loginName ya está registrado", ex.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(emailVerificationTokenRepository, never()).save(any());
    }

    @Test
    void should_throw_conflict_when_email_already_exists() {
        RegisterRequest request = new RegisterRequest(
                "usuario1",
                "usuario1@test.com",
                "Usuario Uno",
                "Password123"
        );

        when(appUserRepository.existsByLoginNameIgnoreCase("usuario1")).thenReturn(false);
        when(appUserRepository.existsByEmailAddressIgnoreCase("usuario1@test.com")).thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> authService.register(request)
        );

        assertEquals("El emailAddress ya está registrado", ex.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(emailVerificationTokenRepository, never()).save(any());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void should_login_successfully_when_credentials_are_correct() {
        LoginRequest request = new LoginRequest("usuario1", "Password123");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setDisplayName("Usuario Uno");
        user.setAccessLevel(AccessLevel.user);
        user.setAccountState(AccountState.active);

        when(appUserRepository.findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase("usuario1", "usuario1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-pass")).thenReturn(true);
        when(jwtService.generateAccessToken(eq(10), eq("usuario1"), eq("user"), anyString()))
                .thenReturn("access-token");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");

        LoginResponse response = authService.login(request, httpServletRequest);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertNotNull(response.user());
        assertEquals(10, response.user().id());
        assertEquals("usuario1", response.user().loginName());
        assertEquals("Usuario Uno", response.user().displayName());
        assertEquals("user", response.user().accessLevel());

        verify(appUserRepository, atLeastOnce()).save(any(AppUser.class));
        verify(userSessionRepository).save(any(UserSession.class));
        verify(jwtService).generateAccessToken(eq(10), eq("usuario1"), eq("user"), anyString());
    }

    @Test
    void should_throw_unauthorized_when_user_not_found() {
        LoginRequest request = new LoginRequest("noexiste", "Password123");

        when(appUserRepository.findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase("noexiste", "noexiste"))
                .thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request, httpServletRequest)
        );

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void should_throw_unauthorized_when_password_is_incorrect() {
        LoginRequest request = new LoginRequest("usuario1", "WrongPassword");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setAccountState(AccountState.active);

        when(appUserRepository.findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase("usuario1", "usuario1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-pass")).thenReturn(false);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request, httpServletRequest)
        );

        assertEquals("Credenciales inválidas", ex.getMessage());
        verify(userSessionRepository, never()).save(any());
    }

    @Test
    void should_throw_unauthorized_when_account_is_deleted() {
        LoginRequest request = new LoginRequest("usuario1", "Password123");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setDeletedAt(LocalDateTime.now());

        when(appUserRepository.findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase("usuario1", "usuario1"))
                .thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request, httpServletRequest)
        );

        assertEquals("Cuenta no disponible", ex.getMessage());
    }

    @Test
    void should_throw_unauthorized_when_account_is_suspended() {
        LoginRequest request = new LoginRequest("usuario1", "Password123");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setAccountState(AccountState.suspended);

        when(appUserRepository.findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase("usuario1", "usuario1"))
                .thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request, httpServletRequest)
        );

        assertEquals("Cuenta suspendida o bloqueada", ex.getMessage());
    }

    @Test
    void should_throw_unauthorized_when_account_is_banned() {
        LoginRequest request = new LoginRequest("usuario1", "Password123");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setAccountState(AccountState.banned);

        when(appUserRepository.findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase("usuario1", "usuario1"))
                .thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request, httpServletRequest)
        );

        assertEquals("Cuenta suspendida o bloqueada", ex.getMessage());
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    void should_refresh_token_successfully_when_token_is_valid() {
        String rawRefreshToken = "refresh-token-ok";
        String refreshHash = sha256Base64(rawRefreshToken);

        UserSession session = new UserSession();
        session.setId(1L);
        session.setUserId(10);
        session.setSessionIdentifier("session-old");
        session.setRefreshTokenHash(refreshHash);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setAccessLevel(AccessLevel.user);

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash))
                .thenReturn(Optional.of(session));
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq(10), eq("usuario1"), eq("user"), anyString()))
                .thenReturn("new-access-token");

        RefreshResponse response = authService.refresh(new RefreshTokenRequest(rawRefreshToken));

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertNotNull(session.getRevokedAt());
        verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void should_throw_unauthorized_when_refresh_token_is_invalid() {
        String rawRefreshToken = "invalid-token";
        String refreshHash = sha256Base64(rawRefreshToken);

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash))
                .thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(new RefreshTokenRequest(rawRefreshToken))
        );

        assertEquals("Refresh token inválido", ex.getMessage());
        verify(jwtService, never()).generateAccessToken(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void should_throw_unauthorized_when_refresh_token_is_expired() {
        String rawRefreshToken = "expired-token";
        String refreshHash = sha256Base64(rawRefreshToken);

        UserSession session = new UserSession();
        session.setUserId(10);
        session.setRefreshTokenHash(refreshHash);
        session.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash))
                .thenReturn(Optional.of(session));

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(new RefreshTokenRequest(rawRefreshToken))
        );

        assertEquals("Refresh token expirado", ex.getMessage());
    }

    @Test
    void should_throw_unauthorized_when_user_not_found_on_refresh() {
        String rawRefreshToken = "valid-format-but-user-deleted";
        String refreshHash = sha256Base64(rawRefreshToken);

        UserSession session = new UserSession();
        session.setUserId(999);
        session.setRefreshTokenHash(refreshHash);
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash))
                .thenReturn(Optional.of(session));
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(new RefreshTokenRequest(rawRefreshToken))
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    void should_logout_successfully() {
        String rawRefreshToken = "refresh-logout";
        String refreshHash = sha256Base64(rawRefreshToken);

        UserSession session = new UserSession();
        session.setId(44L);
        session.setUserId(10);
        session.setRefreshTokenHash(refreshHash);
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash))
                .thenReturn(Optional.of(session));

        MessageResponse response = authService.logout(new LogoutRequest(rawRefreshToken));

        assertEquals("Sesión cerrada correctamente", response.message());
        assertNotNull(session.getRevokedAt());
        verify(userSessionRepository).save(session);
    }

    @Test
    void should_throw_unauthorized_when_logout_with_invalid_token() {
        String rawRefreshToken = "invalid-logout-token";
        String refreshHash = sha256Base64(rawRefreshToken);

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash))
                .thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.logout(new LogoutRequest(rawRefreshToken))
        );

        assertEquals("Refresh token inválido", ex.getMessage());
    }

    // ==================== ME TESTS ====================

    @Test
    void should_return_current_user_info() {
        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setDisplayName("Usuario Uno");
        user.setBioText("Bio");
        user.setAvatarUrl("http://avatar");
        user.setAccessLevel(AccessLevel.user);
        user.setAccountState(AccountState.active);

        mockAuthenticatedUser(user);

        CurrentUserResponse response = authService.me();

        assertNotNull(response);
        assertEquals(10, response.id());
        assertEquals("usuario1", response.loginName());
        assertEquals("usuario1@test.com", response.emailAddress());
        assertEquals("Usuario Uno", response.displayName());
        assertEquals("Bio", response.bioText());
        assertEquals("http://avatar", response.avatarUrl());
        assertEquals("user", response.accessLevel());
        assertEquals("active", response.accountState());
    }

    @Test
    void should_throw_unauthorized_when_not_authenticated_in_me() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.me()
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    // ==================== FORGOT PASSWORD TESTS ====================

    @Test
    void should_create_reset_token_when_email_exists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("usuario1@test.com");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");

        when(appUserRepository.findByEmailAddressIgnoreCase("usuario1@test.com"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(10))
                .thenReturn(List.of());

        MessageResponse response = authService.forgotPassword(request);

        assertEquals("Si el correo existe, se enviaron instrucciones", response.message());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void should_invalidate_old_reset_tokens_before_creating_new_one() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("usuario1@test.com");

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");

        PasswordResetToken old1 = new PasswordResetToken();
        old1.setId(1L);
        old1.setUserId(10);

        PasswordResetToken old2 = new PasswordResetToken();
        old2.setId(2L);
        old2.setUserId(10);

        when(appUserRepository.findByEmailAddressIgnoreCase("usuario1@test.com"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(10))
                .thenReturn(List.of(old1, old2));

        MessageResponse response = authService.forgotPassword(request);

        assertEquals("Si el correo existe, se enviaron instrucciones", response.message());
        assertNotNull(old1.getUsedAt());
        assertNotNull(old2.getUsedAt());
        verify(passwordResetTokenRepository, times(3)).save(any(PasswordResetToken.class));
    }

    @Test
    void should_return_generic_message_when_email_does_not_exist() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("noexiste@test.com");

        when(appUserRepository.findByEmailAddressIgnoreCase("noexiste@test.com"))
                .thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(request);

        assertEquals("Si el correo existe, se enviaron instrucciones", response.message());
        verify(passwordResetTokenRepository, never()).findByUserIdAndUsedAtIsNull(anyInt());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    // ==================== RESET PASSWORD TESTS ====================

    @Test
    void should_reset_password_successfully_with_valid_token() {
        String rawResetToken = "valid-reset-token";
        String tokenHash = sha256Base64(rawResetToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(10);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "old-hashed-pass");

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(Optional.of(resetToken));
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hashed-pass");
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(10)).thenReturn(List.of());

        MessageResponse response = authService.resetPassword(new ResetPasswordRequest(rawResetToken, "NewPassword123"));

        assertEquals("Contraseña actualizada correctamente", response.message());
        assertEquals("new-hashed-pass", user.getPasswordHash());
        assertNotNull(resetToken.getUsedAt());
        verify(appUserRepository).save(any(AppUser.class));
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void should_invalidate_all_sessions_after_password_reset() {
        String rawResetToken = "valid-reset-token";
        String tokenHash = sha256Base64(rawResetToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(10);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "old-hashed-pass");

        UserSession session1 = new UserSession();
        session1.setId(1L);
        UserSession session2 = new UserSession();
        session2.setId(2L);

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(Optional.of(resetToken));
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hashed-pass");
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(10))
                .thenReturn(List.of(session1, session2));

        authService.resetPassword(new ResetPasswordRequest(rawResetToken, "NewPassword123"));

        assertNotNull(session1.getRevokedAt());
        assertNotNull(session2.getRevokedAt());
        verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void should_throw_bad_request_when_reset_token_is_invalid() {
        String rawResetToken = "invalid-reset-token";
        String tokenHash = sha256Base64(rawResetToken);

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(rawResetToken, "NewPassword123"))
        );

        assertEquals("Token inválido", ex.getMessage());
    }

    @Test
    void should_throw_bad_request_when_reset_token_is_expired() {
        String rawResetToken = "expired-reset-token";
        String tokenHash = sha256Base64(rawResetToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(Optional.of(resetToken));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(rawResetToken, "NewPassword123"))
        );

        assertEquals("Token expirado", ex.getMessage());
    }

    @Test
    void should_throw_bad_request_when_reset_token_user_not_found() {
        String rawResetToken = "user-deleted-token";
        String tokenHash = sha256Base64(rawResetToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(999);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(Optional.of(resetToken));
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(rawResetToken, "NewPassword123"))
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    // ==================== VERIFY EMAIL TESTS ====================

    @Test
    void should_verify_email_successfully_with_valid_token() {
        String rawVerificationToken = "valid-verification-token";
        String tokenHash = sha256Base64(rawVerificationToken);

        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setUserId(10);
        emailToken.setExpiresAt(LocalDateTime.now().plusHours(24));

        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");
        user.setAccountState(AccountState.pending_verification);

        when(emailVerificationTokenRepository.findByTokenHashAndVerifiedAtIsNull(tokenHash))
                .thenReturn(Optional.of(emailToken));
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));

        MessageResponse response = authService.verifyEmail(new VerifyEmailRequest(rawVerificationToken));

        assertEquals("Correo confirmado correctamente", response.message());
        assertNotNull(emailToken.getVerifiedAt());
        assertNotNull(user.getEmailVerifiedAt());
        assertEquals(AccountState.active, user.getAccountState());
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void should_throw_bad_request_when_verification_token_is_invalid() {
        String rawVerificationToken = "invalid-verification-token";
        String tokenHash = sha256Base64(rawVerificationToken);

        when(emailVerificationTokenRepository.findByTokenHashAndVerifiedAtIsNull(tokenHash))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest(rawVerificationToken))
        );

        assertEquals("Token inválido", ex.getMessage());
    }

    @Test
    void should_throw_bad_request_when_verification_token_is_expired() {
        String rawVerificationToken = "expired-verification-token";
        String tokenHash = sha256Base64(rawVerificationToken);

        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(emailVerificationTokenRepository.findByTokenHashAndVerifiedAtIsNull(tokenHash))
                .thenReturn(Optional.of(emailToken));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest(rawVerificationToken))
        );

        assertEquals("Token expirado", ex.getMessage());
    }

    @Test
    void should_throw_bad_request_when_verification_user_not_found() {
        String rawVerificationToken = "user-deleted-verification-token";
        String tokenHash = sha256Base64(rawVerificationToken);

        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setUserId(999);
        emailToken.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(emailVerificationTokenRepository.findByTokenHashAndVerifiedAtIsNull(tokenHash))
                .thenReturn(Optional.of(emailToken));
        when(appUserRepository.findById(999)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest(rawVerificationToken))
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    // ==================== INVALIDATE ALL SESSIONS TESTS ====================

    @Test
    void should_invalidate_all_sessions() {
        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");

        UserSession session1 = new UserSession();
        session1.setId(1L);
        UserSession session2 = new UserSession();
        session2.setId(2L);

        mockAuthenticatedUser(user);
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(10))
                .thenReturn(List.of(session1, session2));

        MessageResponse response = authService.invalidateAllSessions();

        assertEquals("Todas las sesiones fueron invalidadas", response.message());
        assertNotNull(session1.getRevokedAt());
        assertNotNull(session2.getRevokedAt());
        verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void should_not_fail_when_invalidating_zero_sessions() {
        AppUser user = buildUser(10, "usuario1", "usuario1@test.com", "hashed-pass");

        mockAuthenticatedUser(user);
        when(userSessionRepository.findByUserIdAndRevokedAtIsNull(10))
                .thenReturn(List.of());

        MessageResponse response = authService.invalidateAllSessions();

        assertEquals("Todas las sesiones fueron invalidadas", response.message());
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    // ==================== HELPER METHODS ====================

    private AppUser buildUser(Integer id, String loginName, String email, String passwordHash) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setLoginName(loginName);
        user.setEmailAddress(email);
        user.setPasswordHash(passwordHash);
        user.setAccessLevel(AccessLevel.user);
        user.setAccountState(AccountState.active);
        return user;
    }

    private void mockAuthenticatedUser(AppUser user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private String sha256Base64(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
