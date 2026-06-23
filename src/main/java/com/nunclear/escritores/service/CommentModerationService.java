package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.HideCommentRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.ContentReport;
import com.nunclear.escritores.entity.StoryComment;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ContentReportRepository;
import com.nunclear.escritores.repository.StoryCommentRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentModerationService {

    private final StoryCommentRepository storyCommentRepository;
    private final ContentReportRepository contentReportRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ModeratedCommentResponse hideComment(Integer id, HideCommentRequest request) {
        StoryComment comment = storyCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        AppUser moderator = getAuthenticatedModeratorOrAdmin();

        comment.setVisibilityState("hidden");
        storyCommentRepository.save(comment);

        contentReportRepository.resolvePendingReportsForComment(
                comment.getId(),
                "reviewed",
                moderator.getId(),
                request.reasonText()
        );

        return new ModeratedCommentResponse(
                comment.getId(),
                comment.getVisibilityState()
        );
    }

    @Transactional
    public ModeratedCommentResponse restoreComment(Integer id) {
        StoryComment comment = storyCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        AppUser moderator = getAuthenticatedModeratorOrAdmin();

        comment.setVisibilityState("visible");
        storyCommentRepository.save(comment);

        contentReportRepository.resolvePendingReportsForComment(
                comment.getId(),
                "reviewed",
                moderator.getId(),
                "Comentario restaurado por moderación"
        );

        return new ModeratedCommentResponse(
                comment.getId(),
                comment.getVisibilityState()
        );
    }

    public PageResponse<HiddenCommentItemResponse> getHiddenComments(int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "updatedAt,desc" : sort);
        Page<StoryComment> result = storyCommentRepository.findByVisibilityStateIgnoreCaseAndDeletedAtIsNull("hidden", pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(comment -> new HiddenCommentItemResponse(
                                comment.getId(),
                                comment.getVisibilityState(),
                                comment.getContent()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<ReportedCommentItemResponse> getReportedComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContentReport> reports = contentReportRepository.findPendingCommentReports(pageable);

        var content = reports.getContent().stream()
                .map(report -> {
                    StoryComment comment = storyCommentRepository.findById(report.getCommentId()).orElse(null);
                    if (comment == null) {
                        return null;
                    }
                    long reportsCount = contentReportRepository.countByCommentIdValue(comment.getId());
                    return new ReportedCommentItemResponse(
                            comment.getId(),
                            reportsCount,
                            comment.getContent()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return new PageResponse<>(
                content,
                reports.getNumber(),
                reports.getSize(),
                reports.getTotalElements(),
                reports.getTotalPages()
        );
    }

    public PageResponse<ModerationQueueItemResponse> getModerationQueue(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContentReport> reports = contentReportRepository.findPendingCommentReports(pageable);

        return new PageResponse<>(
                reports.getContent().stream()
                        .map(report -> new ModerationQueueItemResponse(
                                report.getCommentId(),
                                summarizeReason(report.getReasonText())
                        ))
                        .toList(),
                reports.getNumber(),
                reports.getSize(),
                reports.getTotalElements(),
                reports.getTotalPages()
        );
    }

    private String summarizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Sin motivo especificado";
        }
        String normalized = reason.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private AppUser getAuthenticatedModeratorOrAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No autenticado");
        }

        AppUser user = appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        String role = user.getAccessLevel().name();
        if (!"moderator".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new UnauthorizedException("No autorizado");
        }

        return user;
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
            case "visibilityState" -> "visibilityState";
            default -> "updatedAt";
        };
    }
}