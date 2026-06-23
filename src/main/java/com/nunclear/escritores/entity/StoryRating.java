package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "story_rating",
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_story_rating_user", columnNames = {"story_id", "author_user_id"})
        }
)
@Getter
@Setter
public class StoryRating extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "author_user_id", nullable = false)
    private Integer authorUserId;

    @Column(name = "score_value", nullable = false)
    private Integer scoreValue;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;


}