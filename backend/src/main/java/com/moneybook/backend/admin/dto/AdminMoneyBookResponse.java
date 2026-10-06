package com.moneybook.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminMoneyBookResponse(Long moneyBookUid, String name, Long ownerUserUid,
        String ownerNickname, long memberCount, LocalDateTime createdAt, LocalDateTime lastActivityAt) { }
