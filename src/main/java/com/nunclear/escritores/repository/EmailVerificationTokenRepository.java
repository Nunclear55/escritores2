package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHashAndVerifiedAtIsNull(String tokenHash);
    List<EmailVerificationToken> findByUserIdAndVerifiedAtIsNull(Integer userId);
}