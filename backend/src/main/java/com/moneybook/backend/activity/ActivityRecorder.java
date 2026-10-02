package com.moneybook.backend.activity;

import com.moneybook.backend.activity.event.ActivityEvent;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** 업무 mutation에서 최소 표시 정보만 담은 Activity 이벤트를 발행한다. */
@Component
public class ActivityRecorder {
    private final ApplicationEventPublisher publisher;
    public ActivityRecorder(ApplicationEventPublisher publisher) { this.publisher = publisher; }

    public void record(Long bookUid, Authentication authentication, ActivityType type,
                       ActivityTargetType targetType, Long targetUid, String summary, String metadata) {
        Long actorUid = authentication == null ? null : parseUid(authentication.getName());
        if (actorUid != null) publisher.publishEvent(new ActivityEvent(bookUid, actorUid, type,
                targetType, targetUid, summary, metadata));
    }

    private Long parseUid(String name) {
        try { return Long.valueOf(name); } catch (RuntimeException ignored) { return null; }
    }
}
