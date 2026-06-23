package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Arc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArcRepository extends JpaRepository<Arc, Integer> {

    Page<Arc> findByStoryId(Integer storyId, Pageable pageable);

    List<Arc> findByStoryIdOrderByPositionIndexAscIdAsc(Integer storyId);
}