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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String FIELD_ACCESS_LEVEL = "accessLevel";
    private static final String FIELD_ACCOUNT_STATE = "accountState";
    private static final String FIELD_CREATED_AT = "createdAt";

    private final AppUserRepository appUserRepository;
    private final UserChangeHistoryRepository userChangeHistoryRepository;
    private final UserSessionRepository userSessionRepository;

    public AdminUserAccessLevelResponse updateAccessLevel(Integer id, UpdateUserAccessLevelRequest request) {
        AppUser targetUser = getTargetUser(id);
        AppUser adminUser = getAuthenticatedUser();

        AccessLevel newAccessLevel = parseAccessLevel(request.accessLevel());
        AccessLevel oldAccessLevel = targetUser.getAccessLevel();

        if (oldAccessLevel == newAccessLevel) {
            throw new BadRequestException("El usuario ya tiene ese accessLevel");
        }

        targetUser.setAccessLevel(newAccessLevel);
        AppUser saved = appUserRepository.save(targetUser);

        saveHistory(
                saved.getId(),
                FIELD_ACCESS_LEVEL,
                oldAccessLevel.name(),
                newAccessLevel.name(),
                adminUser.getId()
        );

        return new AdminUserAccessLevelResponse(
                saved.getId(),
                saved.getAccessLevel().name(),
                saved.getUpdatedAt()
        );
    }

    public AdminUserAccountStateResponse updateAccountState(Integer id, UpdateUserAccountStateRequest request) {
        AppUser targetUser = getTargetUser(id);
        AppUser adminUser = getAuthenticatedUser();

        AccountState newAccountState = parseAccountState(request.accountState());
        AccountState oldAccountState = targetUser.getAccountState();

        if (oldAccountState == newAccountState) {
            throw new BadRequestException("El usuario ya tiene ese accountState");
        }

        targetUser.setAccountState(newAccountState);

        if (newAccountState == AccountState.banned) {
            revokeAllSessions(targetUser.getId());
        }

        AppUser saved = appUserRepository.save(targetUser);

        saveHistory(
                saved.getId(),
                FIELD_ACCOUNT_STATE,
                oldAccountState.name(),
                newAccountState.name(),
                adminUser.getId()
        );

        return new AdminUserAccountStateResponse(
                saved.getId(),
                saved.getAccountState().name(),
                saved.getUpdatedAt()
        );
    }

    public PageResponse<AdminUserByRoleItemResponse> listUsersByRole(
            String accessLevel,
            int page,
            int size,
            String sort
    ) {
        AccessLevel parsedAccessLevel = parseAccessLevel(accessLevel);
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? FIELD_CREATED_AT + ",desc" : sort);

        Page<AppUser> result = appUserRepository.findByAccessLevelAndDeletedAtIsNull(parsedAccessLevel, pageable);

        List<AdminUserByRoleItemResponse> content = result.getContent().stream()
                .map(user -> new AdminUserByRoleItemResponse(
                        user.getId(),
                        user.getLoginName(),
                        user.getAccessLevel().name()
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

    public PageResponse<AdminUserByStateItemResponse> listUsersByState(
            String accountState,
            int page,
            int size,
            String sort
    ) {
        AccountState parsedAccountState = parseAccountState(accountState);
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? FIELD_CREATED_AT + ",desc" : sort);

        Page<AppUser> result = appUserRepository.findByAccountStateAndDeletedAtIsNull(parsedAccountState, pageable);

        List<AdminUserByStateItemResponse> content = result.getContent().stream()
                .map(user -> new AdminUserByStateItemResponse(
                        user.getId(),
                        user.getLoginName(),
                        user.getAccountState().name()
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

    public UserHistoryResponse getUserHistory(Integer id) {
        getTargetUser(id);

        Page<UserChangeHistory> historyPage = userChangeHistoryRepository
                .findByUserIdOrderByChangedAtDesc(id, PageRequest.of(0, 100));

        List<UserHistoryEventResponse> events = historyPage.getContent().stream()
                .map(event -> new UserHistoryEventResponse(
                        event.getChangedField(),
                        event.getOldValue(),
                        event.getNewValue(),
                        event.getChangedAt(),
                        event.getChangedByUserId()
                ))
                .toList();

        return new UserHistoryResponse(events);
    }

    private void saveHistory(
            Integer userId,
            String changedField,
            String oldValue,
            String newValue,
            Integer changedByUserId
    ) {
        UserChangeHistory history = new UserChangeHistory();
        history.setUserId(userId);
        history.setChangedField(changedField);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedByUserId(changedByUserId);
        history.setChangedAt(LocalDateTime.now());

        userChangeHistoryRepository.save(history);
    }

    private void revokeAllSessions(Integer userId) {
        userSessionRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(session -> {
                    session.setRevokedAt(LocalDateTime.now());
                    userSessionRepository.save(session);
                });
    }

    private AppUser getTargetUser(Integer id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        return user;
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
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private AccessLevel parseAccessLevel(String accessLevel) {
        try {
            return AccessLevel.valueOf(accessLevel.trim().toLowerCase());
        } catch (Exception ex) {
            throw new BadRequestException("accessLevel inválido");
        }
    }

    private AccountState parseAccountState(String accountState) {
        try {
            return AccountState.valueOf(accountState.trim().toLowerCase());
        } catch (Exception ex) {
            throw new BadRequestException("accountState inválido");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case FIELD_CREATED_AT -> FIELD_CREATED_AT;
            case "updatedAt" -> "updatedAt";
            case "loginName" -> "loginName";
            case FIELD_ACCESS_LEVEL -> FIELD_ACCESS_LEVEL;
            case FIELD_ACCOUNT_STATE -> FIELD_ACCOUNT_STATE;
            default -> FIELD_CREATED_AT;
        };
    }
}