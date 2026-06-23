package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_comment")
@Getter
@Setter
public class StoryComment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "chapter_id")
    private Integer chapterId;

    @Column(name = "author_user_id", nullable = false)
    private Integer authorUserId;

    @Column(name = "parent_comment_id")
    private Integer parentCommentId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "visibility_state", nullable = false, length = 30)
    private String visibilityState = "visible";

    @Column(name = "edited_at")
    private LocalDateTime editedAt;


    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (this.visibilityState == null || this.visibilityState.isBlank()) {
            this.visibilityState = "visible";
        }
    }

}