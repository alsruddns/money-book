package com.moneybook.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminMoneyBookDetailResponse(Long moneyBookUid, String name, Long ownerUserUid,
        String ownerNickname, long memberCount, long adminMemberCount, long categoryCount, long accountCount,
        long transactionCount, long incomeTransactionCount, long expenseTransactionCount,
        long transferCount, long recurringRuleCount, long monthClosingCount,
        LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime lastActivityAt, long recentActivityCount) {
    public AdminMoneyBookDetailResponse(Long moneyBookUid, String name, Long ownerUserUid, String ownerNickname,
            long memberCount, long categoryCount, long accountCount, long transactionCount, long transferCount,
            long monthClosingCount, LocalDateTime createdAt, LocalDateTime lastActivityAt) {
        this(moneyBookUid, name, ownerUserUid, ownerNickname, memberCount, 0, categoryCount, accountCount,
                transactionCount, 0, 0, transferCount, 0, monthClosingCount, createdAt, createdAt,
                lastActivityAt, 0);
    }
}
