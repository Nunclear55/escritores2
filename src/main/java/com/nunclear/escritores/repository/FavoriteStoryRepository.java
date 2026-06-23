package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.StoryFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteStoryRepository extends JpaRepository<StoryFavorite, Integer> {

    Optional<StoryFavorite> findByUserIdAndStoryId(Integer userId, Integer storyId);

    Page<StoryFavorite> findByUserId(Integer userId, Pageable pageable);

    boolean existsByUserIdAndStoryId(Integer userId, Integer storyId);

    long countByStoryId(Integer storyId);

    void deleteByUserIdAndStoryId(Integer userId, Integer storyId);
}