package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.util.AppClock;

import com.nunclear.escritores.dto.request.*;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.*;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ReportService {

    // Mala práctica corregida:
    // strings mágicos repetidos.
    // Tipo: duplicación de literales / baja mantenibilidad.
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REVIEWED = "reviewed";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String REPORT_NOT_FOUND = "Reporte no encontrado";
    private static final String DEFAULT_CREATED_DESC_SORT = "createdAt,desc";

    private final ContentReportRepository contentReportRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryCommentRepository storyCommentRepository;
    private final AppUserRepository appUserRepository;

    public ReportTargetResponse reportStory(CreateStoryReportRequest request) {
        AppUser reporter = getAuthenticatedUser();

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException("Historia no encontrada"));

        ContentReport report = new ContentReport();
        report.setReporterUserId(reporter.getId());
        report.setStoryId(story.getId());
        report.setReasonText(request.reasonText());
        report.setStatusName(STATUS_PENDING);

        ContentReport saved = contentReportRepository.save(report);

        return new ReportTargetResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getChapterId(),
                saved.getCommentId(),
                saved.getTargetUserId(),
                saved.getStatusName()
        );
    }

    public ReportTargetResponse reportChapter(CreateChapterReportRequest request) {
        AppUser reporter = getAuthenticatedUser();

        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new ResourceNotFoundException("Capítulo no encontrado"));

        ContentReport report = new ContentReport();
        report.setReporterUserId(reporter.getId());
        report.setChapterId(chapter.getId());
        report.setStoryId(chapter.getStoryId());
        report.setReasonText(request.reasonText());
        report.setStatusName(STATUS_PENDING);

        ContentReport saved = contentReportRepository.save(report);

        return new ReportTargetResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getChapterId(),
                saved.getCommentId(),
                saved.getTargetUserId(),
                saved.getStatusName()
        );
    }

    public ReportTargetResponse reportComment(CreateCommentReportRequest request) {
        AppUser reporter = getAuthenticatedUser();

        StoryComment comment = storyCommentRepository.findById(request.commentId())
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        ContentReport report = new ContentReport();
        report.setReporterUserId(reporter.getId());
        report.setCommentId(comment.getId());
        report.setStoryId(comment.getStoryId());
        report.setChapterId(comment.getChapterId());
        report.setTargetUserId(comment.getAuthorUserId());
        report.setReasonText(request.reasonText());
        report.setStatusName(STATUS_PENDING);

        ContentReport saved = contentReportRepository.save(report);

        return new ReportTargetResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getChapterId(),
                saved.getCommentId(),
                saved.getTargetUserId(),
                saved.getStatusName()
        );
    }

    public ReportTargetResponse reportUser(CreateUserReportRequest request) {
        AppUser reporter = getAuthenticatedUser();

        AppUser target = appUserRepository.findById(request.targetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        ContentReport report = new ContentReport();
        report.setReporterUserId(reporter.getId());
        report.setTargetUserId(target.getId());
        report.setReasonText(request.reasonText());
        report.setStatusName(STATUS_PENDING);

        ContentReport saved = contentReportRepository.save(report);

        return new ReportTargetResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getChapterId(),
                saved.getCommentId(),
                saved.getTargetUserId(),
                saved.getStatusName()
        );
    }

    public PageResponse<ReportListItemResponse> getPendingReports(int page, int size, String sort) {
        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort
        );
        Page<ContentReport> result = contentReportRepository.findPendingReports(pageable);
        return mapReportPage(result);
    }

    public PageResponse<ReportListItemResponse> getReportsByStatus(String statusName, int page, int size, String sort) {
        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort
        );
        Page<ContentReport> result = contentReportRepository.findByStatusNameIgnoreCase(statusName, pageable);
        return mapReportPage(result);
    }

    public ReportDetailResponse getReportById(Integer id) {
        ContentReport report = contentReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(REPORT_NOT_FOUND));

        return new ReportDetailResponse(
                report.getId(),
                report.getReporterUserId(),
                report.getStoryId(),
                report.getChapterId(),
                report.getCommentId(),
                report.getTargetUserId(),
                report.getReasonText(),
                report.getStatusName(),
                report.getReviewedByUserId(),
                report.getResolutionText()
        );
    }

    public AssignedReportResponse assignReviewer(Integer id, AssignReportReviewerRequest request) {
        getAuthenticatedModeratorOrAdmin();

        ContentReport report = contentReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(REPORT_NOT_FOUND));

        AppUser reviewer = appUserRepository.findById(request.reviewedByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Revisor no encontrado"));

        if (!reviewer.getAccessLevel().isModeratorOrAdmin()) {
            throw new BadRequestException("El usuario asignado debe ser moderator o admin");
        }

        report.setReviewedByUserId(reviewer.getId());
        report.setReviewedAt(AppClock.now());
        report.setStatusName(STATUS_REVIEWED);

        ContentReport saved = contentReportRepository.save(report);

        return new AssignedReportResponse(
                saved.getId(),
                saved.getReviewedByUserId(),
                saved.getStatusName()
        );
    }

    public ResolvedReportResponse reviewReport(Integer id, ReviewReportRequest request) {
        return updateReportStatus(id, request.resolutionText(), STATUS_REVIEWED);
    }

    public ResolvedReportResponse resolveReport(Integer id, ResolveReportRequest request) {
        return updateReportStatus(id, request.resolutionText(), STATUS_RESOLVED);
    }

    public ResolvedReportResponse rejectReport(Integer id, ResolveReportRequest request) {
        return updateReportStatus(id, request.resolutionText(), STATUS_REJECTED);
    }

    private ResolvedReportResponse updateReportStatus(Integer id, String resolutionText, String statusName) {
        AppUser moderator = getAuthenticatedModeratorOrAdmin();

        ContentReport report = contentReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(REPORT_NOT_FOUND));

        report.setReviewedByUserId(moderator.getId());
        report.setReviewedAt(AppClock.now());
        report.setResolutionText(resolutionText);
        report.setStatusName(statusName);

        ContentReport saved = contentReportRepository.save(report);

        return new ResolvedReportResponse(
                saved.getId(),
                saved.getStatusName(),
                saved.getResolutionText()
        );
    }

    public PageResponse<ReportListItemResponse> getHistory(
            Integer targetUserId,
            Integer storyId,
            Integer commentId,
            Integer chapterId,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = buildPageable(
                page,
                size,
                sort == null || sort.isBlank() ? DEFAULT_CREATED_DESC_SORT : sort
        );
        Page<ContentReport> result = contentReportRepository.findHistory(
                targetUserId,
                storyId,
                commentId,
                chapterId,
                pageable
        );
        return mapReportPage(result);
    }

    private PageResponse<ReportListItemResponse> mapReportPage(Page<ContentReport> result) {
        return new PageResponse<>(
                result.getContent().stream()
                        .map(report -> new ReportListItemResponse(
                                report.getId(),
                                report.getStoryId(),
                                report.getChapterId(),
                                report.getCommentId(),
                                report.getTargetUserId(),
                                report.getStatusName()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private AppUser getAuthenticatedUser() {
        return AuthUtils.getAuthenticatedUser(appUserRepository);
    }

    private AppUser getAuthenticatedModeratorOrAdmin() {
        AppUser user = getAuthenticatedUser();
        if (!user.getAccessLevel().isModeratorOrAdmin()) {
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
            case "updatedAt" -> "updatedAt";
            case "statusName" -> "statusName";
            default -> "createdAt";
        };
    }
}