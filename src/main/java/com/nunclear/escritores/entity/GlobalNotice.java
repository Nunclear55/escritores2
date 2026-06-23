package com.nunclear.escritores.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "global_notice")
@Getter
@Setter
public class GlobalNotice extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;


    @PrePersist
    public void prePersist() {
        if (this.isEnabled == null) {
            this.isEnabled = false;
        }
        if (this.archived == null) {
            this.archived = false;
        }
    }

}