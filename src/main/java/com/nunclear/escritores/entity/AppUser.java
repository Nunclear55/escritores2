package com.nunclear.escritores.entity;


import com.nunclear.escritores.converter.AccessLevelConverter;
import com.nunclear.escritores.converter.AccountStateConverter;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "login_name", nullable = false, unique = true, length = 100)
    private String loginName;

    @Column(name = "email_address", nullable = false, unique = true, length = 255)
    private String emailAddress;

    @Column(name = "pending_email_address", length = 255)
    private String pendingEmailAddress;

    @Column(name = "email_change_requested_at")
    private LocalDateTime emailChangeRequestedAt;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Convert(converter = AccessLevelConverter.class)
    @Column(name = "access_level", nullable = false, length = 30)
    private AccessLevel accessLevel = AccessLevel.USER;

    @Convert(converter = AccountStateConverter.class)
    @Column(name = "account_state", nullable = false, length = 30)
    private AccountState accountState = AccountState.PENDING_VERIFICATION;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "bio_text", columnDefinition = "TEXT")
    private String bioText;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;


    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}