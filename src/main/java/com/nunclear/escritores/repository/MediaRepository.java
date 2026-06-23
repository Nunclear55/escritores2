package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Integer> {
    Page<Media> findByChapterId(Integer chapterId, Pageable pageable);
}