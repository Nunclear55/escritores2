package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "story_character")
@Getter
@Setter
public class StoryCharacter extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "character_role_name", length = 255)
    private String characterRoleName;

    @Column(name = "profession", length = 255)
    private String profession;

    @Column(name = "ability", length = 255)
    private String ability;

    @Column(name = "age")
    private Integer age;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "is_alive")
    private Boolean isAlive;

    @Column(name = "story_id", nullable = false)
    private Integer storyId;

    @Column(name = "roles_json", columnDefinition = "json")
    private String rolesJson;

    @Column(name = "image_url", length = 500)
    private String imageUrl;


}