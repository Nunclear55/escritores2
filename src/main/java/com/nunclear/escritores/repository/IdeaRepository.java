package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Idea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface IdeaRepository extends JpaRepository<Idea, Integer> {

    @Query("""
            SELECT i
            FROM Idea i
            WHERE i.storyId = :storyId
              AND (:q IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(i.content, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Idea> findByStoryWithSearch(
            @Param("storyId") Integer storyId,
            @Param("q") String q,
            Pageable pageable
    );
}