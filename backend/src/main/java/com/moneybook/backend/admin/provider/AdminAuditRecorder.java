package com.moneybook.backend.admin.provider;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.admin.event.AdminAuditEvent;
import com.moneybook.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** 민감한 request/response 값을 제외하고 관리자 작업의 표준 audit 이벤트만 발행한다. */
@Component
@RequiredArgsConstructor
public class AdminAuditRecorder {
    private final ApplicationEventPublisher events;
    public void record(User actor, AdminAuditActionType action, AdminAuditTargetType target, Long targetUid,
                       String summary) {
        events.publishEvent(new AdminAuditEvent(actor.getUserUid(), actor.getNickname(), actor.getSystemRole(),
                action, target, targetUid, summary));
    }
}
