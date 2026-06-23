package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    boolean existsByLoginNameIgnoreCase(String loginName);

    boolean existsByEmailAddressIgnoreCase(String emailAddress);

    boolean existsByPendingEmailAddressIgnoreCase(String pendingEmailAddress);

    long countByDeletedAtIsNull();

    Optional<AppUser> findByLoginNameIgnoreCase(String loginName);

    Optional<AppUser> findByEmailAddressIgnoreCase(String emailAddress);

    Optional<AppUser> findByLoginNameIgnoreCaseOrEmailAddressIgnoreCase(String loginName, String emailAddress);



    @Query("""
            SELECT u
            FROM AppUser u
            WHERE u.deletedAt IS NULL
              AND (:q IS NULL OR
                   LOWER(u.loginName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<AppUser> searchUsers(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT u
            FROM AppUser u
            WHERE u.deletedAt IS NULL
            """)
    Page<AppUser> findAllActive(Pageable pageable);

    Page<AppUser> findByAccessLevelAndDeletedAtIsNull(AccessLevel accessLevel, Pageable pageable);

    Page<AppUser> findByAccountStateAndDeletedAtIsNull(AccountState accountState, Pageable pageable);

    long countByDeletedAtIsNullAndAccountState(AccountState accountState);

}