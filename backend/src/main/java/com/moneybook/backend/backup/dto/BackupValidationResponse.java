package com.moneybook.backend.backup.dto;

import java.time.LocalDate;
import java.util.List;

public record BackupValidationResponse(boolean valid, int backupVersion, String moneyBookName,
        int categoryCount, int accountCount, int transactionCount, int transferCount,
        int recurringTransactionCount, int budgetCount, int categoryBudgetCount, int monthClosingCount,
        LocalDate firstTransactionDate, LocalDate lastTransactionDate,
        List<String> warnings, List<String> errors) { }
