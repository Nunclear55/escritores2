package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ContentReportRepository extends JpaRepository<ContentReport, Integer> {

    @Query("""
            SELECT r
            FROM ContentReport r
            WHERE r.commentId = :commentId
            """)
    Page<ContentReport> findByCommentId(@Param("commentId") Integer commentId, Pageable pageable);

    @Query("""
            SELECT r
            FROM ContentReport r
            WHERE r.commentId IS NOT NULL
              AND LOWER(r.statusName) = 'pending'
            """)
    Page<ContentReport> findPendingCommentReports(Pageable pageable);

    @Query("""
            SELECT COUNT(r)
            FROM ContentReport r
            WHERE r.commentId = :commentId
            """)
    long countByCommentIdValue(@Param("commentId") Integer commentId);

    @Query("""
            SELECT COUNT(r)
            FROM ContentReport r
            WHERE r.commentId = :commentId
              AND LOWER(r.statusName) = 'pending'
            """)
    long countPendingByCommentId(@Param("commentId") Integer commentId);

    @Modifying
    @Query("""
            UPDATE ContentReport r
            SET r.statusName = :statusName,
                r.reviewedByUserId = :reviewedByUserId,
                r.reviewedAt = CURRENT_TIMESTAMP,
                r.resolutionText = :resolutionText
            WHERE r.commentId = :commentId
              AND LOWER(r.statusName) = 'pending'
            """)
    int resolvePendingReportsForComment(
            @Param("commentId") Integer commentId,
            @Param("statusName") String statusName,
            @Param("reviewedByUserId") Integer reviewedByUserId,
            @Param("resolutionText") String resolutionText
    );

    Page<ContentReport> findByStatusNameIgnoreCase(String statusName, Pageable pageable);

    @Query("""
            SELECT r
            FROM ContentReport r
            WHERE LOWER(r.statusName) = 'pending'
            """)

    Page<ContentReport> findPendingReports(Pageable pageable);

    @Query("""
            SELECT r
            FROM ContentReport r
            WHERE (:targetUserId IS NULL OR r.targetUserId = :targetUserId)
              AND (:storyId IS NULL OR r.storyId = :storyId)
              AND (:commentId IS NULL OR r.commentId = :commentId)
              AND (:chapterId IS NULL OR r.chapterId = :chapterId)
            """)

    Page<ContentReport> findHistory(
            @Param("targetUserId") Integer targetUserId,
            @Param("storyId") Integer storyId,
            @Param("commentId") Integer commentId,
            @Param("chapterId") Integer chapterId,
            Pageable pageable
    );

    long countByStatusNameIgnoreCase(String statusName);
}

