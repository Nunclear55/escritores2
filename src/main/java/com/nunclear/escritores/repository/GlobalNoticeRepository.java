package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.GlobalNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface GlobalNoticeRepository extends JpaRepository<GlobalNotice, Integer> {

    @Query("""
            SELECT n
            FROM GlobalNotice n
            WHERE n.archived = false
              AND n.isEnabled = true
              AND (n.startsAt IS NULL OR n.startsAt <= :now)
              AND (n.endsAt IS NULL OR n.endsAt >= :now)
            """)
    Page<GlobalNotice> findActiveNotices(@Param("now") LocalDateTime now, Pageable pageable);

    Page<GlobalNotice> findByArchivedFalse(Pageable pageable);

    @Query("""
        SELECT COUNT(n)
        FROM GlobalNotice n
        WHERE n.archived = false
          AND n.isEnabled = true
          AND (n.startsAt IS NULL OR n.startsAt <= :now)
          AND (n.endsAt IS NULL OR n.endsAt >= :now)
        """)
    long countActiveNotices(@Param("now") java.time.LocalDateTime now);
}