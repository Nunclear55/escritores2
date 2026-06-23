package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.UserChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChangeHistoryRepository extends JpaRepository<UserChangeHistory, Long> {
    Page<UserChangeHistory> findByUserIdOrderByChangedAtDesc(Integer userId, Pageable pageable);
}