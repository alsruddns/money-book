package com.moneybook.backend.admin.repository.impl;

import com.moneybook.backend.entity.SystemAdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminAuditJpaRepository extends JpaRepository<SystemAdminAuditLog, Long> {
}
