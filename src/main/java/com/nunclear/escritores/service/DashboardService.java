package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.ContentReport;
import com.nunclear.escritores.entity.GlobalNotice;
import com.nunclear.escritores.entity.StoryComment;
import com.nunclear.escritores.entity.StoryRating;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.*;
import com.nunclear.escritores.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppUserRepository appUserRepository;
    private final StoryRepository storyRepository;
    private final StoryFavoriteRepository storyFavoriteRepository;
    private final UserFollowRepository userFollowRepository;
    private final StoryCommentRepository storyCommentRepository;
    private final StoryRatingRepository storyRatingRepository;
    private final ContentReportRepository contentReportRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final GlobalNoticeRepository globalNoticeRepository;

    public MyDashboardSummaryResponse getMySummary() {
        AppUser currentUser = getAuthenticatedUser();

        long storiesCount = storyRepository.countByOwnerUserId(currentUser.getId());
        long draftStoriesCount = storyRepository.countByOwnerUserIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
                currentUser.getId(), "draft"
        );
        long favoritesCount = storyFavoriteRepository.countByUserId(currentUser.getId());
        long followingCount = userFollowRepository.countByFollowerUserId(currentUser.getId());
        long recentCommentsCount = storyCommentRepository.countByAuthorUserIdAndDeletedAtIsNull(currentUser.getId());

        return new MyDashboardSummaryResponse(
                storiesCount,
                draftStoriesCount,
                favoritesCount,
                followingCount,
                recentCommentsCount
        );
    }

    public PageResponse<RecentCommentDashboardItemResponse> getMyRecentComments(int page, int size, String sort) {
        AppUser currentUser = getAuthenticatedUser();

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<StoryComment> result = storyCommentRepository.findByAuthorUserIdAndDeletedAtIsNull(currentUser.getId(), pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(comment -> new RecentCommentDashboardItemResponse(
                                comment.getId(),
                                comment.getContent(),
                                comment.getCreatedAt()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<MyRatingDashboardItemResponse> getMyRatings(int page, int size, String sort) {
        AppUser currentUser = getAuthenticatedUser();

        Pageable pageable = buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort);
        Page<StoryRating> result = storyRatingRepository.findByAuthorUserId(currentUser.getId(), pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(rating -> new MyRatingDashboardItemResponse(
                                rating.getId(),
                                rating.getStoryId(),
                                rating.getScoreValue()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public AdminDashboardSummaryResponse getAdminSummary() {
        AppUser currentUser = getAuthenticatedUser();
        if (!"admin".equals(currentUser.getAccessLevel().name())) {
            throw new UnauthorizedException("Solo un admin puede ver este panel");
        }

        long usersCount = appUserRepository.countByDeletedAtIsNull();
        long storiesCount = storyRepository.countByArchivedAtIsNull();
        long pendingReportsCount = contentReportRepository.countByStatusNameIgnoreCase("pending");
        long activeSanctionsCount = userSanctionRepository.countByIsActiveTrue();
        long activeNoticesCount = globalNoticeRepository.countActiveNotices(LocalDateTime.now());

        return new AdminDashboardSummaryResponse(
                usersCount,
                storiesCount,
                pendingReportsCount,
                activeSanctionsCount,
                activeNoticesCount
        );
    }

    public PageResponse<SystemActivityItemResponse> getSystemActivity(int page, int size) {
        AppUser currentUser = getAuthenticatedUser();
        String role = currentUser.getAccessLevel().name();
        if (!"moderator".equals(role) && !"admin".equals(role)) {
            throw new UnauthorizedException("No autorizado");
        }

        int fetchSize = Math.max(size, 20);

        Page<ContentReport> reports = contentReportRepository.findAll(
                PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<GlobalNotice> notices = globalNoticeRepository.findAll(
                PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<SystemActivityItemResponse> items = new ArrayList<>();

        for (ContentReport report : reports.getContent()) {
            items.add(new SystemActivityItemResponse(
                    "REPORT_CREATED",
                    report.getId(),
                    report.getCreatedAt()
            ));
        }

        for (GlobalNotice notice : notices.getContent()) {
            items.add(new SystemActivityItemResponse(
                    "GLOBAL_NOTICE_CREATED",
                    notice.getId(),
                    notice.getCreatedAt()
            ));
        }

        items.sort(Comparator.comparing(SystemActivityItemResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        List<SystemActivityItemResponse> pageContent = items.subList(fromIndex, toIndex);

        int totalPages = size == 0 ? 0 : (int) Math.ceil(items.size() / (double) size);

        return new PageResponse<>(
                pageContent,
                page,
                size,
                items.size(),
                totalPages
        );
    }

    private AppUser getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No autenticado");
        }

        return appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
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
            case "updatedAt" -> "updatedAt";
            case "scoreValue" -> "scoreValue";
            default -> "createdAt";
        };
    }
}