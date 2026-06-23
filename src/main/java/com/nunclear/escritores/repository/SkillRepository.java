package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Integer> {

    @Query("""
            SELECT s
            FROM Skill s
            WHERE s.storyId = :storyId
              AND (:categoryName IS NULL OR LOWER(s.categoryName) = LOWER(:categoryName))
            """)
    Page<Skill> findByStoryWithFilters(
            @Param("storyId") Integer storyId,
            @Param("categoryName") String categoryName,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM Skill s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Skill> searchByName(@Param("q") String q, Pageable pageable);
}