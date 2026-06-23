package com.nunclear.escritores.entity;

import com.nunclear.escritores.util.AppClock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "story_favorite",
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_favorite_story", columnNames = {"user_id", "story_id"})
        }
)
@Getter
@Setter
public class StoryFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = AppClock.now();
    }
}