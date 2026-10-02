package com.moneybook.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminMoneyBookDetailResponse(Long moneyBookUid, String name, Long ownerUserUid,
        String ownerNickname, long memberCount, long categoryCount, long accountCount,
        long transactionCount, long transferCount, long monthClosingCount,
        LocalDateTime createdAt, LocalDateTime lastActivityAt) { }
