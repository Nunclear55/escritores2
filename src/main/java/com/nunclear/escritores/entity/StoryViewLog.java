package com.nunclear.escritores.entity;

import com.nunclear.escritores.util.AppClock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_view_log")
@Getter
@Setter
public class StoryViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "chapter_id")
    private Integer chapterId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "visitor_token", length = 100)
    private String visitorToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent_text", length = 500)
    private String userAgentText;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @PrePersist
    public void prePersist() {
        if (this.viewedAt == null) {
            this.viewedAt = AppClock.now();
        }
    }
}