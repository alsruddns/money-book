package com.moneybook.backend.admin.event;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.enums.SystemRole;
import java.time.LocalDateTime;

public record AdminAuditEvent(Long actorUid, String actorNickname, SystemRole actorRole,
        AdminAuditActionType action, AdminAuditTargetType targetType, Long targetUid,
        String summary, LocalDateTime occurredAt) {
    public AdminAuditEvent(Long actorUid, String actorNickname, SystemRole actorRole,
            AdminAuditActionType action, AdminAuditTargetType targetType, Long targetUid, String summary) {
        this(actorUid, actorNickname, actorRole, action, targetType, targetUid, summary, LocalDateTime.now());
    }
}
