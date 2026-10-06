package com.moneybook.backend.admin.event;

import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.entity.SystemAdminAuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/** 업무 transaction commit 이후 독립 transaction으로 관리자 감사행위를 기록한다. */
@Component
@Slf4j
public class AdminAuditEventListener {
    private final AdminAuditRepository audits;
    private final TransactionTemplate transaction;
    public AdminAuditEventListener(AdminAuditRepository audits, PlatformTransactionManager manager) {
        this.audits = audits;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(AdminAuditEvent event) { persist(event); }

    @EventListener
    public void outsideTransaction(AdminAuditEvent event) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            persist(event);
        }
    }

    private void persist(AdminAuditEvent event) {
        try {
            transaction.executeWithoutResult(status -> audits.save(SystemAdminAuditLog.create(
                    event.actorUid(), event.actorNickname(), event.actorRole(), event.action(),
                    event.targetType(), event.targetUid(), event.summary(), event.occurredAt())));
        } catch (RuntimeException exception) {
            log.error("Admin audit save failed actorUserUid={} action={} cause={}",
                    event.actorUid(), event.action(), exception.getClass().getSimpleName());
        }
    }
}
