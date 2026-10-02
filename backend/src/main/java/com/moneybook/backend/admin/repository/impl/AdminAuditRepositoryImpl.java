package com.moneybook.backend.admin.repository.impl;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.entity.SystemAdminAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class AdminAuditRepositoryImpl implements AdminAuditRepository {
    private final AdminAuditJpaRepository jpa;
    @Override public SystemAdminAuditLog save(SystemAdminAuditLog log) { return jpa.save(log); }
    @Override public Page<SystemAdminAuditLog> search(Long actor, AdminAuditActionType action,
            AdminAuditTargetType target, LocalDate start, LocalDate end, Pageable pageable) {
        return jpa.search(actor, action, target, start == null ? null : start.atStartOfDay(),
                end == null ? null : end.plusDays(1).atStartOfDay(), pageable);
    }
}
