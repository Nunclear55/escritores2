package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Getter
@Setter
public class Media extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "media_kind", nullable = false, length = 50)
    private String mediaKind;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(name = "storage_path", length = 500)
    private String storagePath;


}