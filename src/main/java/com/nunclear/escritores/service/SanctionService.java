package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.util.AppClock;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.UserSanction;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.UserSanctionRepository;
import com.nunclear.escritores.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class SanctionService {

    // Mala práctica corregida:
    // string mágico repetido.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String DEFAULT_CREATED_DESC_SORT = "createdAt,desc";

    private final UserSanctionRepository userSanctionRepository;
    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;

    public SanctionResponse createWarning(CreateWarningRequest request) {
        AppUser moderator = getAuthenticatedModeratorOrAdmin();
        AppUser target = getTargetUser(request.targetUserId());

        UserSanction sanction = new UserSanction();
        sanction.setTargetUserId(target.getId());
        sanction.setAppliedByUserId(moderator.getId());
        sanction.setSanctionKind("warning");
        sanction.setReasonText(request.reasonText());
        sanction.setIsActive(true);

        UserSanction saved = userSanctionRepository.save(sanction);

        return new SanctionResponse(
                saved.getId(),
                saved.getTargetUserId(),
                saved.getSanctionKind(),
                saved.getIsActive()
        );
    }

    public SanctionResponse createTemporaryBan(CreateTemporaryBanRequest request) {
        AppUser moderator = getAuthenticatedModeratorOrAdmin();
        AppUser target = getTargetUser(request.targetUserId());

        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("endsAt debe ser posterior a startsAt");
        }

        UserSanction sanction = new UserSanction();
        sanction.setTargetUserId(target.getId());
        sanction.setAppliedByUserId(moderator.getId());
        sanction.setSanctionKind("temporary_ban");
        sanction.setReasonText(request.reasonText());
        sanction.setStartsAt(request.startsAt());
        sanction.setEndsAt(request.endsAt());
        sanction.setIsActive(true);

        UserSanction saved = userSanctionRepository.save(sanction);

        target.setAccountState(com.nunclear.escritores.enums.AccountState.SUSPENDED);
        appUserRepository.save(target);
        revokeAllSessions(target.getId());

        return new SanctionResponse(
                saved.getId(),
                saved.getTargetUserId(),
                saved.getSanctionKind(),
                saved.getIsActive()
        );
    }

    public SanctionResponse createPermanentBan(CreatePermanentBanRequest request) {
        AppUser admin = getAuthenticatedAdminOnly();
        AppUser target = getTargetUser(request.targetUserId());

        UserSanction sanction = new UserSanction();
        sanction.setTargetUserId(target.getId());
        sanction.setAppliedByUserId(admin.getId());
        sanction.setSanctionKind("permanent_ban");
        sanction.setReasonText(request.reasonText());
        sanction.setIsActive(true);

        UserSanction saved = userSanctionRepository.save(sanction);

        target.setAccountState(com.nunclear.escritores.enums.AccountState.BANNED);
        appUserRepository.save(target);
        revokeAllSessions(target.getId());

        return new SanctionResponse(
                saved.getId(),
                saved.getTargetUserId(),
                saved.getSanctionKind(),
                saved.getIsActive()
        );
    }

    public SanctionResponse liftSanction(Integer id, LiftSanctionRequest request) {
        getAuthenticatedModeratorOrAdmin();

        UserSanction sanction = userSanctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sanción no encontrada"));

        AppUser target = getTargetUser(sanction.getTargetUserId());

        sanction.setIsActive(false);
        sanction.setReasonText(sanction.getReasonText() + "\n[LIFT] " + request.reasonText());

        UserSanction saved = userSanctionRepository.save(sanction);

        if ("temporary_ban".equals(sanction.getSanctionKind()) || "permanent_ban".equals(sanction.getSanctionKind())) {
            target.setAccountState(com.nunclear.escritores.enums.AccountState.ACTIVE);
            appUserRepository.save(target);
        }

        return new SanctionResponse(
                saved.getId(),
                saved.getTargetUserId(),
                saved.getSanctionKind(),
                saved.getIsActive()
        );
    }

    public PageResponse<SanctionListItemResponse> getSanctionsByUser(Integer userId, int page, int size, String sort) {
        getAuthenticatedModeratorOrAdmin();
        getTargetUser(userId);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort, "createdAt", "updatedAt", "sanctionKind");
        Page<UserSanction> result = userSanctionRepository.findByTargetUserId(userId, pageable);

        return mapPage(result);
    }

    public PageResponse<SanctionListItemResponse> getMySanctions(int page, int size, String sort) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort, "createdAt", "updatedAt", "sanctionKind");
        Page<UserSanction> result = userSanctionRepository.findByTargetUserId(currentUser.getId(), pageable);

        return mapPage(result);
    }

    public PageResponse<SanctionListItemResponse> getActiveSanctions(int page, int size, String sort) {
        getAuthenticatedModeratorOrAdmin();

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort, "createdAt", "updatedAt", "sanctionKind");
        Page<UserSanction> result = userSanctionRepository.findByIsActiveTrue(pageable);

        return mapPage(result);
    }

    private PageResponse<SanctionListItemResponse> mapPage(Page<UserSanction> result) {
        return new PageResponse<>(
                result.getContent().stream()
                        .map(sanction -> new SanctionListItemResponse(
                                sanction.getId(),
                                sanction.getTargetUserId(),
                                sanction.getSanctionKind(),
                                sanction.getIsActive()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void revokeAllSessions(Integer userId) {
        userSessionRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(session -> {
                    session.setRevokedAt(AppClock.now());
                    userSessionRepository.save(session);
                });
    }

    private AppUser getTargetUser(Integer id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private AppUser getAuthenticatedModeratorOrAdmin() {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);
        if (!user.getAccessLevel().isModeratorOrAdmin()) {
            throw new UnauthorizedException("No autorizado");
        }

        return user;
    }

    private AppUser getAuthenticatedAdminOnly() {
        AppUser user = AuthUtils.getAuthenticatedUser(appUserRepository);
        if (!user.getAccessLevel().isAdmin()) {
            throw new UnauthorizedException("Solo un admin puede realizar esta acción");
        }
        return user;
    }

}