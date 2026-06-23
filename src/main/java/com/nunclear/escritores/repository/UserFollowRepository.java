package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Integer> {

    long countByFollowedUserId(Integer followedUserId);

    Page<UserFollow> findByFollowerUserId(Integer followerUserId, Pageable pageable);

    Page<UserFollow> findByFollowedUserId(Integer followedUserId, Pageable pageable);

    boolean existsByFollowerUserIdAndFollowedUserId(Integer followerUserId, Integer followedUserId);

    Optional<UserFollow> findByFollowerUserIdAndFollowedUserId(Integer followerUserId, Integer followedUserId);

    void deleteByFollowerUserIdAndFollowedUserId(Integer followerUserId, Integer followedUserId);

    long countByFollowerUserId(Integer followerUserId);
}