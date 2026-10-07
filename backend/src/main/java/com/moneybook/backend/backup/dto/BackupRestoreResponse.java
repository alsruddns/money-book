package com.moneybook.backend.backup.dto;

public record BackupRestoreResponse(Long moneyBookUid, String name, int categoryCount, int accountCount,
        int transactionCount, int transferCount, int recurringTransactionCount, int budgetCount,
        int categoryBudgetCount, int monthClosingCount) { }
