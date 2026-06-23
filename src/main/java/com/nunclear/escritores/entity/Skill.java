package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill")
@Getter
@Setter
public class Skill extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "level_value")
    private Integer levelValue;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;


}