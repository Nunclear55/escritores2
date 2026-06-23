package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "story_event")
@Getter
@Setter
public class StoryEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_on")
    private LocalDate eventOn;

    @Column(name = "importance")
    private Integer importance;

    @Column(name = "event_kind", length = 100)
    private String eventKind;

    @Column(name = "tags_json", columnDefinition = "json")
    private String tagsJson;

    @Column(name = "linked_characters_json", columnDefinition = "json")
    private String linkedCharactersJson;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "chapter_id")
    private Integer chapterId;


}