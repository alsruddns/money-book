package com.moneybook.backend.activity.event;

import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;

import java.time.LocalDateTime;

/** 변경 성공 시점에 발행되는 민감정보가 없는 최소 Activity 스냅샷. */
public record ActivityEvent(Long moneyBookUid, Long actorUserUid, ActivityType activityType,
                            ActivityTargetType targetType, Long targetUid, String summary,
                            String metadataJson, LocalDateTime occurredAt) {
    public ActivityEvent(Long moneyBookUid, Long actorUserUid, ActivityType activityType,
                         ActivityTargetType targetType, Long targetUid, String summary, String metadataJson) {
        this(moneyBookUid, actorUserUid, activityType, targetType, targetUid, summary,
                metadataJson, LocalDateTime.now());
    }
}
