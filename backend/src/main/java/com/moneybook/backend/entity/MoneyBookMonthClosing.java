package com.moneybook.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_month_closings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookMonthClosing extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closing_uid")
    private Long closingUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(name = "\"month\"", nullable = false)
    private int month;

    @Column(name = "income", nullable = false, precision = 19, scale = 2)
    private BigDecimal income;

    @Column(name = "expense", nullable = false, precision = 19, scale = 2)
    private BigDecimal expense;

    @Column(name = "transaction_count", nullable = false)
    private long transactionCount;

    @Column(name = "previous_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal previousIncome;

    @Column(name = "previous_expense", nullable = false, precision = 19, scale = 2)
    private BigDecimal previousExpense;

    @Column(name = "budget_configured", nullable = false)
    private boolean budgetConfigured;

    @Column(name = "total_budget", precision = 19, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "closed_by_user_uid", nullable = false)
    private Long closedByUserUid;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    private MoneyBookMonthClosing(MoneyBook moneyBook, int year, int month, BigDecimal income,
                                  BigDecimal expense, long transactionCount, BigDecimal previousIncome,
                                  BigDecimal previousExpense, boolean budgetConfigured, BigDecimal totalBudget,
                                  Long closedByUserUid, LocalDateTime closedAt) {
        this.moneyBook = Objects.requireNonNull(moneyBook);
        this.year = year;
        this.month = month;
        this.income = Objects.requireNonNull(income);
        this.expense = Objects.requireNonNull(expense);
        this.transactionCount = transactionCount;
        this.previousIncome = Objects.requireNonNull(previousIncome);
        this.previousExpense = Objects.requireNonNull(previousExpense);
        this.budgetConfigured = budgetConfigured;
        this.totalBudget = totalBudget;
        this.closedByUserUid = Objects.requireNonNull(closedByUserUid);
        this.closedAt = Objects.requireNonNull(closedAt);
    }

    public static MoneyBookMonthClosing create(MoneyBook book, int year, int month, BigDecimal income,
                                               BigDecimal expense, long count, BigDecimal previousIncome,
                                               BigDecimal previousExpense, boolean budgetConfigured,
                                               BigDecimal totalBudget, Long userUid, LocalDateTime time) {
        return new MoneyBookMonthClosing(book, year, month, income, expense, count, previousIncome,
                previousExpense, budgetConfigured, totalBudget, userUid, time);
    }
}
