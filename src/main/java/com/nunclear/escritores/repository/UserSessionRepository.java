package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);
    List<UserSession> findByUserIdAndRevokedAtIsNull(Integer userId);
}