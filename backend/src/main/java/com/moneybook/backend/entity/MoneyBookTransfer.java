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
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_transfers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookTransfer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_uid")
    private Long transferUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_uid", nullable = false)
    private MoneyBookAccount fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_uid", nullable = false)
    private MoneyBookAccount toAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "memo", length = 500)
    private String memo;

    private MoneyBookTransfer(MoneyBook moneyBook, MoneyBookAccount fromAccount, MoneyBookAccount toAccount,
                              BigDecimal amount, LocalDate transferDate, String memo) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        change(fromAccount, toAccount, amount, transferDate, memo);
    }

    public static MoneyBookTransfer create(MoneyBook moneyBook, MoneyBookAccount fromAccount,
                                           MoneyBookAccount toAccount, BigDecimal amount,
                                           LocalDate transferDate, String memo) {
        return new MoneyBookTransfer(moneyBook, fromAccount, toAccount, amount, transferDate, memo);
    }

    public void change(MoneyBookAccount fromAccount, MoneyBookAccount toAccount,
                       BigDecimal amount, LocalDate transferDate, String memo) {
        Objects.requireNonNull(fromAccount, "fromAccount");
        Objects.requireNonNull(toAccount, "toAccount");
        Objects.requireNonNull(transferDate, "transferDate");
        if (fromAccount.getAccountUid().equals(toAccount.getAccountUid())) {
            throw new IllegalArgumentException("Transfer accounts must differ");
        }
        Long bookUid = moneyBook.getMoneyBookUid();
        if (!bookUid.equals(fromAccount.getMoneyBook().getMoneyBookUid())
                || !bookUid.equals(toAccount.getMoneyBook().getMoneyBookUid())) {
            throw new IllegalArgumentException("Transfer account belongs to another book");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Invalid transfer amount");
        }
        if (memo != null && memo.length() > 500) {
            throw new IllegalArgumentException("Transfer memo exceeds 500 characters");
        }
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.transferDate = transferDate;
        this.memo = memo;
    }
}
