package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.UserSanction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Integer> {

    Page<UserSanction> findByTargetUserId(Integer targetUserId, Pageable pageable);

    Page<UserSanction> findByTargetUserIdAndIsActiveTrue(Integer targetUserId, Pageable pageable);

    Page<UserSanction> findByIsActiveTrue(Pageable pageable);

    long countByIsActiveTrue();

}