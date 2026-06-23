package com.nunclear.escritores.service;

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
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GlobalNoticeService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String NOTICE_NOT_FOUND = "Comunicado no encontrado";
    private static final String ROLE_ADMIN = "admin";

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

            String role = user.getAccessLevel().name();
            if (!"moderator".equals(role) && !ROLE_ADMIN.equals(role)) {
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
        Page<GlobalNotice> result = globalNoticeRepository.findActiveNotices(LocalDateTime.now(), pageable);

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
        LocalDateTime now = LocalDateTime.now();

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
        if (!ROLE_ADMIN.equals(user.getAccessLevel().name())) {
            throw new UnauthorizedException("Solo un admin puede realizar esta acción");
        }
        return user;
    }

    private AppUser getAuthenticatedModeratorOrAdmin() {
        AppUser user = getAuthenticatedUser();
        String role = user.getAccessLevel().name();

        if (!"moderator".equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new UnauthorizedException("No autorizado");
        }

        return user;
    }

    private AppUser getAuthenticatedUser() {
        // Mala práctica corregida:
        // acceso directo a getAuthentication().getPrincipal() sin validar null.
        // Tipo: riesgo de NullPointerException / falta de validación defensiva.
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

    private AppUser tryGetAuthenticatedUser() {
        // Mala práctica corregida:
        // catch vacío.
        // Tipo: swallowing exceptions / ocultamiento silencioso de errores.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return appUserRepository.findById(userDetails.getId()).orElse(null);
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