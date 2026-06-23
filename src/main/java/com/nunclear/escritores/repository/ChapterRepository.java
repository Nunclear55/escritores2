package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.storyId = :storyId
              AND c.archivedAt IS NULL
            ORDER BY c.positionIndex ASC, c.id ASC
            """)
    List<Chapter> findAllActiveByStoryIdOrderByPosition(@Param("storyId") Integer storyId);

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.storyId = :storyId
              AND c.archivedAt IS NULL
              AND LOWER(c.publicationState) = 'published'
            ORDER BY c.positionIndex ASC, c.id ASC
            """)
    List<Chapter> findPublishedByStoryIdOrderByPosition(@Param("storyId") Integer storyId);

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.storyId = :storyId
              AND c.archivedAt IS NULL
            """)
    Page<Chapter> findPageActiveByStoryId(@Param("storyId") Integer storyId, Pageable pageable);

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.storyId = :storyId
              AND c.archivedAt IS NULL
              AND LOWER(c.publicationState) = 'published'
            """)
    Page<Chapter> findPagePublishedByStoryId(@Param("storyId") Integer storyId, Pageable pageable);

    Page<Chapter> findByStoryIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
            Integer storyId,
            String publicationState,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.storyId = :storyId
              AND c.archivedAt IS NULL
              AND LOWER(c.publicationState) = 'draft'
            """)
    Page<Chapter> findDraftsByStoryId(@Param("storyId") Integer storyId, Pageable pageable);

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.archivedAt IS NULL
              AND LOWER(c.publicationState) = 'draft'
              AND c.storyId IN :storyIds
            """)
    Page<Chapter> findDraftsByStoryIds(@Param("storyIds") List<Integer> storyIds, Pageable pageable);

    @Query("""
            SELECT c
            FROM Chapter c
            WHERE c.archivedAt IS NULL
              AND LOWER(c.publicationState) = 'published'
              AND (:storyId IS NULL OR c.storyId = :storyId)
              AND (
                    LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(c.subtitle, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(c.content, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Chapter> searchPublishedChapters(
            @Param("q") String q,
            @Param("storyId") Integer storyId,
            Pageable pageable
    );
}