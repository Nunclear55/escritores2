package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "character_skill")
@Getter
@Setter
public class CharacterSkill extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "story_character_id", nullable = false)
    private Integer storyCharacterId;

    @Column(name = "skill_id", nullable = false)
    private Integer skillId;

    @Column(name = "proficiency")
    private Integer proficiency;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;


}