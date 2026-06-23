package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Integer> {

    Optional<Story> findBySlugText(String slugText);

    boolean existsBySlugText(String slugText);

    long countByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
            Integer ownerUserId,
            String visibilityState,
            String publicationState
    );

    Page<Story> findByOwnerUserIdAndVisibilityStateIgnoreCaseAndPublicationStateIgnoreCase(
            Integer ownerUserId,
            String visibilityState,
            String publicationState,
            Pageable pageable
    );

    Page<Story> findByVisibilityStateIgnoreCaseAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
            String visibilityState,
            String publicationState,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM Story s
            WHERE s.archivedAt IS NULL
              AND LOWER(s.visibilityState) = LOWER(:visibilityState)
              AND LOWER(s.publicationState) = LOWER(:publicationState)
              AND (
                    LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Story> searchPublicStories(
            @Param("q") String q,
            @Param("visibilityState") String visibilityState,
            @Param("publicationState") String publicationState,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM Story s
            WHERE s.ownerUserId = :ownerUserId
              AND s.archivedAt IS NULL
              AND (
                    LOWER(s.visibilityState) = 'public'
                AND LOWER(s.publicationState) = 'published'
              )
            """)
    Page<Story> findPublicPublishedByOwner(@Param("ownerUserId") Integer ownerUserId, Pageable pageable);

    @Query("""
            SELECT s
            FROM Story s
            WHERE s.ownerUserId = :ownerUserId
              AND s.archivedAt IS NULL
            """)
    Page<Story> findAllVisibleForOwner(@Param("ownerUserId") Integer ownerUserId, Pageable pageable);

    Page<Story> findByOwnerUserIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(
            Integer ownerUserId,
            String publicationState,
            Pageable pageable
    );

    Page<Story> findByOwnerUserIdAndArchivedAtIsNotNull(
            Integer ownerUserId,
            Pageable pageable
    );
    List<Story> findByOwnerUserId(Integer ownerUserId);


    long countByOwnerUserId(Integer ownerUserId);

    long countByOwnerUserIdAndPublicationStateIgnoreCaseAndArchivedAtIsNull(Integer ownerUserId, String publicationState);

    long countByArchivedAtIsNull();
}