package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.util.AppClock;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.enums.AccountState;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    // Mala práctica corregida:
    // duplicación de literales ("magic strings").
    // Tipo: baja mantenibilidad / riesgo de inconsistencias.
    private static final String USER_NOT_FOUND = "Usuario no encontrado";
    private static final String SORT_CREATED_AT = "createdAt";

    private final AppUserRepository appUserRepository;
    private final StoryRepository storyRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getUserById(Integer id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        validatePublicReadable(user);

        return new UserProfileResponse(
                user.getId(),
                user.getLoginName(),
                user.getDisplayName(),
                user.getBioText(),
                user.getAvatarUrl(),
                user.getAccessLevel().getValue(),
                user.getCreatedAt()
        );
    }

    public CurrentUserResponse getMyProfile() {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);

        return new CurrentUserResponse(
                user.getId(),
                user.getLoginName(),
                user.getEmailAddress(),
                user.getDisplayName(),
                user.getBioText(),
                user.getAvatarUrl(),
                user.getAccessLevel().getValue(),
                user.getAccountState().getValue()
        );
    }

    public PageResponse<UserListItemResponse> listUsers(int page, int size, String sort) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sort, SORT_CREATED_AT, "updatedAt", "displayName", "loginName");
        Page<AppUser> result = appUserRepository.findAllActive(pageable);

        List<UserListItemResponse> content = result.getContent().stream()
                .map(user -> new UserListItemResponse(
                        user.getId(),
                        user.getLoginName(),
                        user.getDisplayName(),
                        user.getAccessLevel().getValue(),
                        user.getAccountState().getValue()
                ))
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<UserSearchItemResponse> searchUsers(String q, int page, int size, String sort) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "displayName,asc" : sort, SORT_CREATED_AT, "updatedAt", "displayName", "loginName");
        Page<AppUser> result = appUserRepository.searchUsers(q, pageable);

        List<UserSearchItemResponse> content = result.getContent().stream()
                .map(user -> new UserSearchItemResponse(
                        user.getId(),
                        user.getLoginName(),
                        user.getDisplayName(),
                        user.getAvatarUrl()
                ))
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public UpdateMyProfileResponse updateMyProfile(UpdateMyProfileRequest request) {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);

        user.setDisplayName(request.displayName());
        user.setBioText(request.bioText());
        user.setAvatarUrl(request.avatarUrl());

        AppUser saved = appUserRepository.save(user);

        return new UpdateMyProfileResponse(
                saved.getId(),
                saved.getDisplayName(),
                saved.getBioText(),
                saved.getAvatarUrl(),
                saved.getUpdatedAt()
        );
    }

    public AvatarResponse changeAvatar(ChangeAvatarRequest request) {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);
        user.setAvatarUrl(request.avatarUrl());
        AppUser saved = appUserRepository.save(user);

        return new AvatarResponse(saved.getAvatarUrl(), saved.getUpdatedAt());
    }

    public MessageResponse changePassword(ChangePasswordRequest request) {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("La contraseña actual no es correcta");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("La nueva contraseña no puede ser igual a la anterior");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        for (UserSession session : userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            session.setRevokedAt(AppClock.now());
            userSessionRepository.save(session);
        }

        return new MessageResponse("Contraseña actualizada correctamente");
    }

    public ChangeEmailResponse changeEmail(ChangeEmailRequest request) {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("La contraseña no es correcta");
        }

        if (appUserRepository.existsByEmailAddressIgnoreCase(request.newEmailAddress())) {
            throw new BadRequestException("El nuevo correo ya está en uso");
        }

        if (appUserRepository.existsByPendingEmailAddressIgnoreCase(request.newEmailAddress())) {
            throw new BadRequestException("Ese correo ya está pendiente de confirmación por otro usuario");
        }

        user.setPendingEmailAddress(request.newEmailAddress());
        user.setEmailChangeRequestedAt(AppClock.now());
        appUserRepository.save(user);

        return new ChangeEmailResponse(
                "Cambio de correo solicitado",
                user.getPendingEmailAddress()
        );
    }

    public MessageResponse deactivateMyAccount() {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);

        user.setAccountState(AccountState.BANNED);
        user.setDeletedAt(AppClock.now());
        appUserRepository.save(user);

        for (UserSession session : userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            session.setRevokedAt(AppClock.now());
            userSessionRepository.save(session);
        }

        return new MessageResponse("Cuenta desactivada correctamente");
    }

    public PublicAuthorProfileResponse getPublicAuthorProfile(Integer id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        validatePublicReadable(user);

        long followersCount = userFollowRepository.countByFollowedUserId(id);
        long storiesCount = storyRepository.countByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
                id, "public", "published"
        );

        return new PublicAuthorProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getBioText(),
                user.getAvatarUrl(),
                followersCount,
                storiesCount
        );
    }

    public PageResponse<UserStoryItemResponse> getPublicStoriesByAuthor(Integer id, int page, int size, String sort) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        validatePublicReadable(user);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? SORT_CREATED_AT + ",desc" : sort, SORT_CREATED_AT, "updatedAt", "displayName", "loginName");

        Page<Story> result = storyRepository.findByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
                id, "public", "published", pageable
        );

        List<UserStoryItemResponse> content = result.getContent().stream()
                .map(story -> new UserStoryItemResponse(
                        story.getId(),
                        story.getTitle(),
                        story.getSlugText(),
                        story.getPublicationState(),
                        story.getVisibilityState()
                ))
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void validatePublicReadable(AppUser user) {
        if (user.getDeletedAt() != null) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }

        if (user.getAccountState() == AccountState.BANNED) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }
    }

}