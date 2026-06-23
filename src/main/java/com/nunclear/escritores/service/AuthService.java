package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ConflictException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import com.nunclear.escritores.security.CustomUserDetails;
import com.nunclear.escritores.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String USER_NOT_FOUND = "Usuario no encontrado";

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.access-expiration-seconds}")
    private long accessExpirationSeconds;

    @Value("${app.jwt.refresh-expiration-seconds}")
    private long refreshExpirationSeconds;

    public RegisterResponse register(RegisterRequest request) {
        if (appUserRepository.existsByLoginNameIgnoreCase(request.loginName())) {
            throw new ConflictException("El loginName ya está registrado");
        }

        if (appUserRepository.existsByEmailAddressIgnoreCase(request.emailAddress())) {
            throw new ConflictException("El emailAddress ya está registrado");
        }

        AppUser user = new AppUser();
        user.setLoginName(request.loginName());
        user.setEmailAddress(request.emailAddress());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAccessLevel(AccessLevel.user);
        user.setAccountState(AccountState.pending_verification);

        AppUser saved = appUserRepository.save(user);

        String rawVerificationToken = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(saved.getId());
        token.setTokenHash(hash(rawVerificationToken));
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        log.debug("TOKEN VERIFICACION DEV: {}", rawVerificationToken);

        return new RegisterResponse(
                saved.getId(),
                saved.getLoginName(),
                saved.getEmailAddress(),
                saved.getDisplayName(),
                saved.getAccessLevel().name(),
                saved.getAccountState().name(),
                saved.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        AppUser user = appUserRepository
                .findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase(request.loginOrEmail(), request.loginOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (user.getDeletedAt() != null) {
            throw new UnauthorizedException("Cuenta no disponible");
        }

        if (user.getAccountState() == AccountState.suspended || user.getAccountState() == AccountState.banned) {
            throw new UnauthorizedException("Cuenta suspendida o bloqueada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);

        String sessionId = UUID.randomUUID().toString();
        String rawRefreshToken = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getLoginName(),
                user.getAccessLevel().name(),
                sessionId
        );

        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setSessionIdentifier(sessionId);
        session.setRefreshTokenHash(hash(rawRefreshToken));
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationSeconds));
        session.setIpAddress(httpRequest.getRemoteAddr());
        session.setUserAgentText(httpRequest.getHeader("User-Agent"));
        userSessionRepository.save(session);

        return new LoginResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                accessExpirationSeconds,
                new UserSummaryResponse(
                        user.getId(),
                        user.getLoginName(),
                        user.getDisplayName(),
                        user.getAccessLevel().name()
                )
        );
    }

    public RefreshResponse refresh(RefreshTokenRequest request) {
        String refreshHash = hash(request.refreshToken());

        UserSession session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        AppUser user = appUserRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException(USER_NOT_FOUND));

        String newSessionId = UUID.randomUUID().toString();
        String newRefreshToken = UUID.randomUUID().toString() + "." + UUID.randomUUID();

        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        UserSession newSession = new UserSession();
        newSession.setUserId(user.getId());
        newSession.setSessionIdentifier(newSessionId);
        newSession.setRefreshTokenHash(hash(newRefreshToken));
        newSession.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationSeconds));
        userSessionRepository.save(newSession);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getLoginName(),
                user.getAccessLevel().name(),
                newSessionId
        );

        return new RefreshResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                accessExpirationSeconds
        );
    }

    public MessageResponse logout(LogoutRequest request) {
        String refreshHash = hash(request.refreshToken());

        UserSession session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        return new MessageResponse("Sesión cerrada correctamente");
    }

    public CurrentUserResponse me() {
        AppUser user = getAuthenticatedUser();

        return new CurrentUserResponse(
                user.getId(),
                user.getLoginName(),
                user.getEmailAddress(),
                user.getDisplayName(),
                user.getBioText(),
                user.getAvatarUrl(),
                user.getAccessLevel().name(),
                user.getAccountState().name()
        );
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        appUserRepository.findByEmailAddressIgnoreCase(request.emailAddress()).ifPresent(user -> {
            for (PasswordResetToken t : passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())) {
                t.setUsedAt(LocalDateTime.now());
                passwordResetTokenRepository.save(t);
            }

            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(token);

            log.debug("TOKEN RESET DEV: {}", rawToken);
        });

        return new MessageResponse("Si el correo existe, se enviaron instrucciones");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNull(hash(request.resetToken()))
                .orElseThrow(() -> new BadRequestException("Token inválido"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expirado");
        }

        AppUser user = appUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        for (UserSession session : userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            session.setRevokedAt(LocalDateTime.now());
            userSessionRepository.save(session);
        }

        return new MessageResponse("Contraseña actualizada correctamente");
    }

    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHashAndVerifiedAtIsNull(hash(request.verificationToken()))
                .orElseThrow(() -> new BadRequestException("Token inválido"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expirado");
        }

        AppUser user = appUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));

        token.setVerifiedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(token);

        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setAccountState(AccountState.active);
        appUserRepository.save(user);

        return new MessageResponse("Correo confirmado correctamente");
    }

    public MessageResponse invalidateAllSessions() {
        AppUser user = getAuthenticatedUser();

        for (UserSession session : userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            session.setRevokedAt(LocalDateTime.now());
            userSessionRepository.save(session);
        }

        return new MessageResponse("Todas las sesiones fueron invalidadas");
    }

    private AppUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("No autenticado");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No autenticado");
        }

        return appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException(USER_NOT_FOUND));
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}