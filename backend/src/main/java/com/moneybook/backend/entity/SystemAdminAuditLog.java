package com.moneybook.backend.entity;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.enums.SystemRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/** 수정 또는 삭제하지 않는 서비스 운영 행위 기록. */
@Entity
@Table(name = "system_admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemAdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_audit_log_uid")
    private Long adminAuditLogUid;
    @Column(name = "actor_user_uid", nullable = false)
    private Long actorUserUid;
    @Column(name = "actor_nickname", nullable = false, length = 50)
    private String actorNickname;
    @Enumerated(EnumType.STRING) @Column(name = "actor_system_role", nullable = false, length = 30)
    private SystemRole actorSystemRole;
    @Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false, length = 50)
    private AdminAuditActionType actionType;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 30)
    private AdminAuditTargetType targetType;
    @Column(name = "target_uid")
    private Long targetUid;
    @Column(name = "summary", nullable = false, length = 200)
    private String summary;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public static SystemAdminAuditLog create(Long actorUid, String nickname, SystemRole role,
            AdminAuditActionType action, AdminAuditTargetType target, Long targetUid,
            String summary, LocalDateTime occurredAt) {
        var row = new SystemAdminAuditLog();
        row.actorUserUid=actorUid; row.actorNickname=nickname; row.actorSystemRole=role;
        row.actionType=action; row.targetType=target; row.targetUid=targetUid;
        row.summary=summary; row.occurredAt=occurredAt;
        return row;
    }
}
