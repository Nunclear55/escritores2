package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    @Query("""
            SELECT i
            FROM Item i
            WHERE i.storyId = :storyId
              AND (:name IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Page<Item> findByStoryWithNameFilter(
            @Param("storyId") Integer storyId,
            @Param("name") String name,
            Pageable pageable
    );
}