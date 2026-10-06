package com.moneybook.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminActivityResponse(Long activityUid, Long moneyBookUid, String moneyBookName,
        Long actorUserUid, String actorNickname, String activityType, String targetType,
        Long targetUid, String summary, String metadataJson, LocalDateTime occurredAt) { }
