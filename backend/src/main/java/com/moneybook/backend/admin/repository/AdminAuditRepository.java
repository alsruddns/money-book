package com.moneybook.backend.admin.repository;

import com.moneybook.backend.entity.SystemAdminAuditLog;
import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface AdminAuditRepository {
    SystemAdminAuditLog save(SystemAdminAuditLog log);
    Page<SystemAdminAuditLog> search(Long actorUid, Long targetUserUid, AdminAuditActionType action,
            AdminAuditTargetType target, LocalDate start, LocalDate end, Pageable pageable);
}
