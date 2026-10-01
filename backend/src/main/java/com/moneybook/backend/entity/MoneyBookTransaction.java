package com.moneybook.backend.entity;

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
@Table(name = "money_book_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_uid")
    private Long transactionUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_uid", nullable = false)
    private MoneyBookCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_uid", nullable = false)
    private MoneyBookAccount account;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "recurring_transaction_uid")
    private Long recurringTransactionUid;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    private MoneyBookTransaction(MoneyBook moneyBook, TransactionType transactionType, BigDecimal amount,
                                 LocalDate transactionDate, MoneyBookCategory category,
                                 MoneyBookAccount account, String memo) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        change(transactionType, amount, transactionDate, category, account, memo);
    }

    public static MoneyBookTransaction create(MoneyBook moneyBook, TransactionType transactionType,
                                              BigDecimal amount, LocalDate transactionDate,
                                              MoneyBookCategory category, MoneyBookAccount account, String memo) {
        return new MoneyBookTransaction(moneyBook, transactionType, amount, transactionDate, category, account, memo);
    }

    public static MoneyBookTransaction createRecurring(MoneyBook moneyBook, TransactionType transactionType,
                                                       BigDecimal amount, LocalDate scheduledDate,
                                                       MoneyBookCategory category, MoneyBookAccount account,
                                                       String memo, Long recurringTransactionUid) {
        MoneyBookTransaction transaction = new MoneyBookTransaction(moneyBook, transactionType, amount,
                scheduledDate, category, account, memo);
        transaction.recurringTransactionUid = Objects.requireNonNull(recurringTransactionUid, "recurringTransactionUid");
        transaction.scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate");
        return transaction;
    }

    public void change(TransactionType transactionType, BigDecimal amount, LocalDate transactionDate,
                       MoneyBookCategory category, MoneyBookAccount account, String memo) {
        Objects.requireNonNull(transactionType, "transactionType");
        Objects.requireNonNull(transactionDate, "transactionDate");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(account, "account");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Invalid transaction amount");
        }
        if (memo != null && memo.length() > 500) {
            throw new IllegalArgumentException("memo exceeds 500 characters");
        }
        if (!moneyBook.getMoneyBookUid().equals(category.getMoneyBook().getMoneyBookUid())
                || !moneyBook.getMoneyBookUid().equals(account.getMoneyBook().getMoneyBookUid())
                || transactionType != category.getTransactionType()) {
            throw new IllegalArgumentException("Transaction references are inconsistent");
        }
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.category = category;
        this.account = account;
        this.memo = memo;
    }
}
