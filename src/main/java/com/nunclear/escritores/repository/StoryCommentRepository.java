package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.StoryComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryCommentRepository extends JpaRepository<StoryComment, Integer> {

    Page<StoryComment> findByStoryIdAndParentCommentIdIsNullAndDeletedAtIsNull(Integer storyId, Pageable pageable);

    Page<StoryComment> findByChapterIdAndDeletedAtIsNull(Integer chapterId, Pageable pageable);

    Page<StoryComment> findByParentCommentIdAndDeletedAtIsNull(Integer parentCommentId, Pageable pageable);

    Page<StoryComment> findByVisibilityStateIgnoreCaseAndDeletedAtIsNull(String visibilityState, Pageable pageable);

    long countByChapterIdAndDeletedAtIsNull(Integer chapterId);

    Page<StoryComment> findByAuthorUserIdAndDeletedAtIsNull(Integer authorUserId, Pageable pageable);

    long countByAuthorUserIdAndDeletedAtIsNull(Integer authorUserId);
}