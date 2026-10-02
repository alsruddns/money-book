package com.moneybook.backend.admin.repository.impl;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.entity.SystemAdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

interface AdminAuditJpaRepository extends JpaRepository<SystemAdminAuditLog, Long> {
    @Query(value = "select a from SystemAdminAuditLog a where (:actor is null or a.actorUserUid=:actor) " +
            "and (:action is null or a.actionType=:action) and (:target is null or a.targetType=:target) " +
            "and (:start is null or a.occurredAt>=:start) and (:end is null or a.occurredAt<:end)",
            countQuery = "select count(a.adminAuditLogUid) from SystemAdminAuditLog a where (:actor is null or a.actorUserUid=:actor) " +
                    "and (:action is null or a.actionType=:action) and (:target is null or a.targetType=:target) " +
                    "and (:start is null or a.occurredAt>=:start) and (:end is null or a.occurredAt<:end)")
    Page<SystemAdminAuditLog> search(@Param("actor") Long actor, @Param("action") AdminAuditActionType action,
            @Param("target") AdminAuditTargetType target, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, Pageable pageable);
}
