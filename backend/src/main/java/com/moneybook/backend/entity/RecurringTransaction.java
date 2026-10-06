package com.moneybook.backend.entity;

import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_recurring_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTransaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurring_transaction_uid")
    private Long recurringTransactionUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_uid", nullable = false)
    private MoneyBookCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_uid", nullable = false)
    private MoneyBookAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private RecurringFrequency frequency;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_generated_date")
    private LocalDate lastGeneratedDate;

    private RecurringTransaction(MoneyBook moneyBook, TransactionType type, BigDecimal amount,
                                 MoneyBookCategory category, MoneyBookAccount account,
                                 RecurringFrequency frequency, Integer dayOfMonth, Integer dayOfWeek,
                                 LocalDate startDate, LocalDate endDate, String memo) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        change(type, amount, category, account, frequency, dayOfMonth, dayOfWeek, startDate, endDate, memo);
        this.active = true;
    }

    public static RecurringTransaction create(MoneyBook moneyBook, TransactionType type, BigDecimal amount,
                                              MoneyBookCategory category, MoneyBookAccount account,
                                              RecurringFrequency frequency, Integer dayOfMonth, Integer dayOfWeek,
                                              LocalDate startDate, LocalDate endDate, String memo) {
        return new RecurringTransaction(moneyBook, type, amount, category, account,
                frequency, dayOfMonth, dayOfWeek, startDate, endDate, memo);
    }

    public void change(TransactionType type, BigDecimal amount, MoneyBookCategory category,
                       MoneyBookAccount account, RecurringFrequency frequency,
                       Integer dayOfMonth, Integer dayOfWeek,
                       LocalDate startDate, LocalDate endDate, String memo) {
        Objects.requireNonNull(type, "transactionType");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(startDate, "startDate");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Invalid recurring amount");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid recurring date range");
        }
        if ((frequency == RecurringFrequency.MONTHLY
                && (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31 || dayOfWeek != null))
                || (frequency == RecurringFrequency.WEEKLY
                && (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7 || dayOfMonth != null))) {
            throw new IllegalArgumentException("Invalid recurring schedule day");
        }
        if (memo != null && memo.length() > 500) {
            throw new IllegalArgumentException("Recurring memo exceeds 500 characters");
        }
        Long bookUid = moneyBook.getMoneyBookUid();
        if (!bookUid.equals(category.getMoneyBook().getMoneyBookUid())
                || !bookUid.equals(account.getMoneyBook().getMoneyBookUid())
                || type != category.getTransactionType()) {
            throw new IllegalArgumentException("Recurring references are inconsistent");
        }
        this.transactionType = type;
        this.amount = amount;
        this.category = category;
        this.account = account;
        this.frequency = frequency;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.startDate = startDate;
        this.endDate = endDate;
        this.memo = memo;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    /** This is display metadata; generated transactions remain the source for idempotency. */
    public void recordGeneratedThrough(LocalDate scheduledDate) {
        if (lastGeneratedDate == null || scheduledDate.isAfter(lastGeneratedDate)) {
            lastGeneratedDate = scheduledDate;
        }
    }
}
