package com.moneybook.backend.backup.dto;

import java.util.List;

/** Versioned DTO contract; it contains only MoneyBook business data, never users or credentials. */
public record MoneyBookBackupDocument(int backupVersion, String exportedAt, BookData moneyBook,
        SettingData setting, List<CategoryData> categories, List<AccountData> accounts,
        List<TransactionData> transactions, List<TransferData> transfers,
        List<RecurringData> recurringTransactions, List<BudgetData> budgets,
        List<ClosingData> monthClosings, List<Long> orphanRecurringSourceUids) {
    public static final int BACKUP_VERSION = 1;
    public record BookData(String name) { }
    public record SettingData(String weekStartDay) { }
    public record CategoryData(Long categoryUid, String name, String transactionType, int sortOrder) { }
    public record AccountData(Long accountUid, String name, String accountType, int sortOrder) { }
    public record TransactionData(Long transactionUid, String transactionType, String amount, String transactionDate,
            Long categoryUid, Long accountUid, String memo, Long recurringTransactionUid, String scheduledDate) { }
    public record TransferData(Long transferUid, Long fromAccountUid, Long toAccountUid, String amount,
            String transferDate, String memo) { }
    public record RecurringData(Long recurringTransactionUid, String transactionType, String amount, Long categoryUid,
            Long accountUid, String frequency, Integer dayOfMonth, Integer dayOfWeek, String startDate,
            String endDate, String memo, boolean active, String lastGeneratedDate) { }
    public record BudgetData(Long budgetUid, int year, int month, String totalBudget,
            List<CategoryBudgetData> categories) { }
    public record CategoryBudgetData(Long categoryUid, String amount) { }
    public record ClosingData(int year, int month, String income, String expense, long transactionCount,
            String previousIncome, String previousExpense, boolean budgetConfigured, String totalBudget,
            String closedAt) { }
}
