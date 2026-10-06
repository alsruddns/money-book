package com.moneybook.backend.admin.dto;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.entity.SystemAdminAuditLog;
import java.time.LocalDateTime;

public record AdminAuditLogResponse(Long adminAuditLogUid, Long actorUserUid, String actorNickname,
        SystemRole actorSystemRole, AdminAuditActionType actionType, AdminAuditTargetType targetType,
        Long targetUid, String summary, LocalDateTime occurredAt) {
    public static AdminAuditLogResponse from(SystemAdminAuditLog row) {
        return new AdminAuditLogResponse(row.getAdminAuditLogUid(), row.getActorUserUid(), row.getActorNickname(),
                row.getActorSystemRole(), row.getActionType(), row.getTargetType(), row.getTargetUid(),
                row.getSummary(), row.getOccurredAt());
    }
}
