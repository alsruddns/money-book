package com.moneybook.backend.activity.dto;

import com.moneybook.backend.entity.MoneyBookActivity;
import java.time.LocalDateTime;

public record ActivityResponse(Long activityUid, Long actorUserUid, String actorNickname, String activityType,
                               String targetType, Long targetUid, String summary, String metadataJson,
                               LocalDateTime occurredAt) {
    public static ActivityResponse from(MoneyBookActivity row) {
        return new ActivityResponse(row.getActivityUid(), row.getActorUserUid(), row.getActorNickname(),
                row.getActivityType().name(), row.getTargetType().name(), row.getTargetUid(), row.getSummary(),
                row.getMetadataJson(), row.getOccurredAt());
    }
}
