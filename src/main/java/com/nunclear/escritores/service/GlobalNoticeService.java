package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.util.AppClock;

import com.nunclear.escritores.dto.request.CreateGlobalNoticeRequest;
import com.nunclear.escritores.dto.request.UpdateGlobalNoticeRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.GlobalNotice;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.GlobalNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GlobalNoticeService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String NOTICE_NOT_FOUND = "Comunicado no encontrado";

    private final GlobalNoticeRepository globalNoticeRepository;
    private final AppUserRepository appUserRepository;

    public GlobalNoticeResponse createNotice(CreateGlobalNoticeRequest request) {
        getAuthenticatedAdmin();

        validateDates(request.startsAt(), request.endsAt());

        GlobalNotice notice = new GlobalNotice();
        notice.setTitle(request.title());
        notice.setMessageText(request.messageText());
        notice.setIsEnabled(request.isEnabled());
        notice.setStartsAt(request.startsAt());
        notice.setEndsAt(request.endsAt());
        notice.setArchived(false);

        GlobalNotice saved = globalNoticeRepository.save(notice);

        return new GlobalNoticeResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getMessageText(),
                saved.getIsEnabled()
        );
    }

    public GlobalNoticeResponse getNoticeById(Integer id) {
        GlobalNotice notice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_NOT_FOUND));

        if (!isPubliclyReadable(notice)) {
            AppUser user = tryGetAuthenticatedUser();
            if (user == null) {
                throw new ResourceNotFoundException(NOTICE_NOT_FOUND);
            }

            if (!user.getAccessLevel().isModeratorOrAdmin()) {
                throw new ResourceNotFoundException(NOTICE_NOT_FOUND);
            }
        }

        return new GlobalNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getMessageText(),
                notice.getIsEnabled()
        );
    }

    public PageResponse<GlobalNoticeListItemResponse> getActiveNotices(int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "startsAt,desc" : sort);
        Page<GlobalNotice> result = globalNoticeRepository.findActiveNotices(AppClock.now(), pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(notice -> new GlobalNoticeListItemResponse(
                                notice.getId(),
                                notice.getTitle(),
                                notice.getMessageText(),
                                notice.getIsEnabled()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<GlobalNoticeListItemResponse> getHistory(int page, int size, String sort) {
        getAuthenticatedModeratorOrAdmin();

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<GlobalNotice> result = globalNoticeRepository.findByArchivedFalse(pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(notice -> new GlobalNoticeListItemResponse(
                                notice.getId(),
                                notice.getTitle(),
                                notice.getMessageText(),
                                notice.getIsEnabled()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public GlobalNoticeResponse updateNotice(Integer id, UpdateGlobalNoticeRequest request) {
        getAuthenticatedAdmin();

        validateDates(request.startsAt(), request.endsAt());

        GlobalNotice notice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_NOT_FOUND));

        notice.setTitle(request.title());
        notice.setMessageText(request.messageText());
        notice.setStartsAt(request.startsAt());
        notice.setEndsAt(request.endsAt());

        GlobalNotice saved = globalNoticeRepository.save(notice);

        return new GlobalNoticeResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getMessageText(),
                saved.getIsEnabled()
        );
    }

    public GlobalNoticeToggleResponse enableNotice(Integer id) {
        getAuthenticatedAdmin();

        GlobalNotice notice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_NOT_FOUND));

        notice.setIsEnabled(true);
        GlobalNotice saved = globalNoticeRepository.save(notice);

        return new GlobalNoticeToggleResponse(saved.getId(), saved.getIsEnabled());
    }

    public GlobalNoticeToggleResponse disableNotice(Integer id) {
        getAuthenticatedAdmin();

        GlobalNotice notice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_NOT_FOUND));

        notice.setIsEnabled(false);
        GlobalNotice saved = globalNoticeRepository.save(notice);

        return new GlobalNoticeToggleResponse(saved.getId(), saved.getIsEnabled());
    }

    public GlobalNoticeArchiveResponse archiveNotice(Integer id) {
        getAuthenticatedAdmin();

        GlobalNotice notice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_NOT_FOUND));

        notice.setArchived(true);
        notice.setIsEnabled(false);

        GlobalNotice saved = globalNoticeRepository.save(notice);

        return new GlobalNoticeArchiveResponse(saved.getId(), saved.getArchived());
    }

    private boolean isPubliclyReadable(GlobalNotice notice) {
        LocalDateTime now = AppClock.now();

        return Boolean.TRUE.equals(notice.getIsEnabled())
                && Boolean.FALSE.equals(notice.getArchived())
                && (notice.getStartsAt() == null || !notice.getStartsAt().isAfter(now))
                && (notice.getEndsAt() == null || !notice.getEndsAt().isBefore(now));
    }

    private void validateDates(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BadRequestException("endsAt debe ser posterior a startsAt");
        }
    }

    private AppUser getAuthenticatedAdmin() {
        AppUser user = getAuthenticatedUser();
        if (!user.getAccessLevel().isAdmin()) {
            throw new UnauthorizedException("Solo un admin puede realizar esta acción");
        }
        return user;
    }

    private AppUser getAuthenticatedModeratorOrAdmin() {
        AppUser user = getAuthenticatedUser();
        if (!user.getAccessLevel().isModeratorOrAdmin()) {
            throw new UnauthorizedException("No autorizado");
        }

        return user;
    }

    private AppUser getAuthenticatedUser() {
        return AuthUtils.getAuthenticatedUser(appUserRepository);
    }

    private AppUser tryGetAuthenticatedUser() {
        return AuthUtils.tryGetAuthenticatedUser(appUserRepository);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(field)));
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "title" -> "title";
            default -> "startsAt";
        };
    }
}