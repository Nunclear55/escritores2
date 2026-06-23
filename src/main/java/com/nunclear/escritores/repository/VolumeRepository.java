package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Volume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VolumeRepository extends JpaRepository<Volume, Integer> {

    Optional<Volume> findByIdAndStoryId(Integer id, Integer storyId);

    Page<Volume> findByStoryId(Integer storyId, Pageable pageable);

    List<Volume> findByStoryIdOrderByPositionIndexAscIdAsc(Integer storyId);

    Page<Volume> findByArcId(Integer arcId, Pageable pageable);
}