package com.moneybook.backend.activity.repository.impl;

import com.moneybook.backend.entity.MoneyBookActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

interface ActivityJpaRepository extends JpaRepository<MoneyBookActivity, Long> {
    @Query(value = "select a from MoneyBookActivity a where (:book is null or a.moneyBookUid = :book) " +
            "and (:start is null or a.occurredAt >= :start) and (:end is null or a.occurredAt < :end) " +
            "and (:actor is null or a.actorUserUid = :actor) " +
            "and (:type is null or a.activityType = :type) and (:target is null or a.targetType = :target)",
            countQuery = "select count(a.activityUid) from MoneyBookActivity a where (:book is null or a.moneyBookUid = :book) " +
                    "and (:start is null or a.occurredAt >= :start) and (:end is null or a.occurredAt < :end) " +
                    "and (:actor is null or a.actorUserUid = :actor) " +
                    "and (:type is null or a.activityType = :type) and (:target is null or a.targetType = :target)")
    Page<MoneyBookActivity> search(@Param("book") Long book, @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end, @Param("actor") Long actor,
                                   @Param("type") com.moneybook.backend.enums.ActivityType type,
                                   @Param("target") com.moneybook.backend.enums.ActivityTargetType target,
                                   Pageable pageable);
}
