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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uq_money_book_categories_book_type_name",
                columnNames = {"money_book_uid", "transaction_type", "name"}),
        @UniqueConstraint(name = "uq_money_book_categories_uid_book_type",
                columnNames = {"category_uid", "money_book_uid", "transaction_type"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_uid")
    private Long categoryUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private MoneyBookCategory(MoneyBook moneyBook, String name, TransactionType transactionType, int sortOrder) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        this.transactionType = Objects.requireNonNull(transactionType, "transactionType");
        change(name, sortOrder);
    }

    public static MoneyBookCategory create(MoneyBook moneyBook, String name,
                                            TransactionType transactionType, int sortOrder) {
        return new MoneyBookCategory(moneyBook, name, transactionType, sortOrder);
    }

    public void change(String name, int sortOrder) {
        if (name == null || name.isBlank() || name.length() > 100 || sortOrder < 0) {
            throw new IllegalArgumentException("Invalid category name or sort order");
        }
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
