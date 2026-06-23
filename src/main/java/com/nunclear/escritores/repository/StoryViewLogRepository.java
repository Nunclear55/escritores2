package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.StoryViewLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StoryViewLogRepository extends JpaRepository<StoryViewLog, Integer> {

    long countByStoryId(Integer storyId);

    long countByChapterId(Integer chapterId);

    @Query("""
            SELECT COALESCE(COUNT(v), 0)
            FROM StoryViewLog v
            JOIN Story s ON s.id = v.storyId
            WHERE s.ownerUserId = :userId
            """)
    long countViewsByAuthor(@Param("userId") Integer userId);

    @Query("""
            SELECT v.storyId, COUNT(v)
            FROM StoryViewLog v
            GROUP BY v.storyId
            """)
    Page<Object[]> findTopViewedStories(Pageable pageable);
}