package com.nunclear.escritores.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chapter")
@Getter
@Setter
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "published_on")
    private LocalDate publishedOn;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "volume_id")
    private Integer volumeId;

    @Column(name = "position_index")
    private Integer positionIndex;

    @Column(name = "reading_minutes")
    private Integer readingMinutes;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "publication_state", nullable = false, length = 30)
    private String publicationState = "draft";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}