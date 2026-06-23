package com.nunclear.escritores.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_follow")
@Getter
@Setter
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "follower_user_id", nullable = false)
    private Integer followerUserId;

    @Column(name = "followed_user_id", nullable = false)
    private Integer followedUserId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}