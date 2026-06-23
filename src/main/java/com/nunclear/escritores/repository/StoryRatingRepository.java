package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.StoryRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoryRatingRepository extends JpaRepository<StoryRating, Integer> {

    Page<StoryRating> findByStoryId(Integer storyId, Pageable pageable);

    Optional<StoryRating> findByStoryIdAndAuthorUserId(Integer storyId, Integer authorUserId);

    @Query("""
            SELECT AVG(r.scoreValue)
            FROM StoryRating r
            WHERE r.storyId = :storyId
            """)
    Double findAverageScoreByStoryId(@Param("storyId") Integer storyId);

    long countByStoryId(Integer storyId);

    Page<StoryRating> findByAuthorUserId(Integer authorUserId, Pageable pageable);
}