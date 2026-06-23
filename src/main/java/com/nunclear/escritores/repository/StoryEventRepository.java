package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.StoryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StoryEventRepository extends JpaRepository<StoryEvent, Integer> {

    @Query("""
            SELECT e
            FROM StoryEvent e
            WHERE e.storyId = :storyId
              AND (:eventKind IS NULL OR LOWER(e.eventKind) = LOWER(:eventKind))
              AND (:importance IS NULL OR e.importance = :importance)
            """)
    Page<StoryEvent> findByStoryWithFilters(
            @Param("storyId") Integer storyId,
            @Param("eventKind") String eventKind,
            @Param("importance") Integer importance,
            Pageable pageable
    );

    Page<StoryEvent> findByChapterId(Integer chapterId, Pageable pageable);

    @Query("""
            SELECT e
            FROM StoryEvent e
            WHERE (:q IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:tag IS NULL OR LOWER(COALESCE(e.tagsJson, '')) LIKE LOWER(CONCAT('%', :tag, '%')))
            """)
    Page<StoryEvent> searchEvents(
            @Param("q") String q,
            @Param("tag") String tag,
            Pageable pageable
    );
}