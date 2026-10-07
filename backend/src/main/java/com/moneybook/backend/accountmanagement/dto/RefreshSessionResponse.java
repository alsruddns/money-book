package com.moneybook.backend.accountmanagement.dto;

import java.time.LocalDateTime;

/** 공개할 수 있는 세션 메타데이터만 담으며 토큰과 해시는 포함하지 않는다. */
public record RefreshSessionResponse(Long sessionUid, boolean current, String userAgent, String ipAddress,
                                     LocalDateTime createdAt, LocalDateTime lastUsedAt,
                                     LocalDateTime expiresAt) { }
