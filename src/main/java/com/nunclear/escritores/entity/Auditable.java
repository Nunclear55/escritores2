package com.nunclear.escritores.entity;

import com.nunclear.escritores.util.AppClock;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Base class that provides auditing fields for entity classes.  It defines
 * <code>createdAt</code> and <code>updatedAt</code> columns and automatically
 * initializes and updates them via JPA lifecycle callbacks.  Entities
 * requiring these timestamps should extend this class to avoid
 * duplicating the same fields and methods.  Subclasses can override
 * {@link #onCreate()} and {@link #onUpdate()} to add additional logic while
 * still invoking <code>super.onCreate()</code> or <code>super.onUpdate()</code>.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class Auditable {

    /**
     * Timestamp of when the entity was created.  Mapped to the
     * <code>created_at</code> column in the database.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Timestamp of when the entity was last updated.  Mapped to the
     * <code>updated_at</code> column in the database.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Sets both <code>createdAt</code> and <code>updatedAt</code> to the
     * current timestamp just before the entity is persisted for the
     * first time.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = AppClock.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Updates the <code>updatedAt</code> timestamp just before the entity
     * is updated.  Subclasses overriding this method should call
     * <code>super.onUpdate()</code> to preserve this behavior.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = AppClock.now();
    }
}