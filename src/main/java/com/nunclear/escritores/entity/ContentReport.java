package com.nunclear.escritores.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_report")
@Getter
@Setter
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reporter_user_id", nullable = false)
    private Integer reporterUserId;

    @Column(name = "target_user_id")
    private Integer targetUserId;

    @Column(name = "story_id")
    private Integer storyId;

    @Column(name = "chapter_id")
    private Integer chapterId;

    @Column(name = "comment_id")
    private Integer commentId;

    @Column(name = "reason_text", nullable = false, columnDefinition = "TEXT")
    private String reasonText;

    @Column(name = "status_name", nullable = false, length = 30)
    private String statusName = "pending";

    @Column(name = "reviewed_by_user_id")
    private Integer reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolution_text", columnDefinition = "TEXT")
    private String resolutionText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.statusName == null || this.statusName.isBlank()) {
            this.statusName = "pending";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}