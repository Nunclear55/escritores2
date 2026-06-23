package com.nunclear.escritores.entity;

import com.nunclear.escritores.util.AppClock;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Base class for token entities such as email verification and password reset
 * tokens.  It centralizes the common fields (identifier, user id,
 * token hash, expiration and creation timestamps) and initialization
 * logic, reducing duplication across token classes.  Subclasses
 * should extend this class and add any additional fields (such as
 * verification or usage timestamps).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the user associated with this token.
     */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /**
     * Hash representation of the token.  Stored instead of the
     * plaintext token for security.
     */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    /**
     * Timestamp when the token expires.  After this time the token is
     * considered invalid.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Timestamp when the token was created.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Initializes the <code>createdAt</code> field just before
     * persisting the token.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = AppClock.now();
    }
}