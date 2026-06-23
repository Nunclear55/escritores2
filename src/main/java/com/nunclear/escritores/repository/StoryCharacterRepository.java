package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.StoryCharacter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StoryCharacterRepository extends JpaRepository<StoryCharacter, Integer> {

    @Query("""
            SELECT c
            FROM StoryCharacter c
            WHERE c.storyId = :storyId
              AND (:isAlive IS NULL OR c.isAlive = :isAlive)
              AND (:characterRoleName IS NULL OR LOWER(c.characterRoleName) = LOWER(:characterRoleName))
            """)
    Page<StoryCharacter> findByStoryWithFilters(
            @Param("storyId") Integer storyId,
            @Param("isAlive") Boolean isAlive,
            @Param("characterRoleName") String characterRoleName,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM StoryCharacter c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<StoryCharacter> searchByName(@Param("q") String q, Pageable pageable);
}