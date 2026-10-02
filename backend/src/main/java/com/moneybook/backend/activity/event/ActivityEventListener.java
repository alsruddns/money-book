package com.moneybook.backend.activity.event;

import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.entity.MoneyBookActivity;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 성공적으로 commit된 업무 이벤트만 독립 트랜잭션으로 immutable activity에 기록한다. */
@Component
@Slf4j
public class ActivityEventListener {
    private final ActivityRepository activities;
    private final UserRepository users;
    private final TransactionTemplate transactionTemplate;

    public ActivityEventListener(ActivityRepository activities, UserRepository users,
                                 PlatformTransactionManager transactionManager) {
        this.activities = activities;
        this.users = users;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ActivityEvent event) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                String nickname = users.findById(event.actorUserUid())
                        .map(user -> user.getNickname()).orElse("알 수 없는 사용자");
                activities.save(MoneyBookActivity.create(event.moneyBookUid(), event.actorUserUid(), nickname,
                        event.activityType(), event.targetType(), event.targetUid(), event.summary(),
                        event.metadataJson(), event.occurredAt()));
            });
        } catch (RuntimeException exception) {
            log.error("Activity save failed moneyBookUid={} actorUserUid={} type={} cause={}",
                    event.moneyBookUid(), event.actorUserUid(), event.activityType(), exception.getClass().getSimpleName());
        }
    }
}
