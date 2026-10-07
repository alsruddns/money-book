package com.moneybook.backend.entity;

import com.moneybook.backend.enums.AccountType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_money_book_accounts_book_name", columnNames = {"money_book_uid", "name"}),
        @UniqueConstraint(name = "uq_money_book_accounts_uid_book", columnNames = {"account_uid", "money_book_uid"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_uid")
    private Long accountUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private MoneyBookAccount(MoneyBook moneyBook, String name, AccountType accountType, int sortOrder) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        change(name, accountType, sortOrder);
    }

    public static MoneyBookAccount create(MoneyBook moneyBook, String name,
                                           AccountType accountType, int sortOrder) {
        return new MoneyBookAccount(moneyBook, name, accountType, sortOrder);
    }

    public void change(String name, AccountType accountType, int sortOrder) {
        if (name == null || name.isBlank() || name.length() > 100 || sortOrder < 0) {
            throw new IllegalArgumentException("Invalid account name or sort order");
        }
        this.name = name;
        this.accountType = Objects.requireNonNull(accountType, "accountType");
        this.sortOrder = sortOrder;
    }
}
