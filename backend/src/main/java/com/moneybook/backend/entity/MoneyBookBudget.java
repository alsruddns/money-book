package com.moneybook.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_budgets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookBudget extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_uid")
    private Long budgetUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(name = "\"month\"", nullable = false)
    private int month;

    @Column(name = "total_budget", precision = 19, scale = 2)
    private BigDecimal totalBudget;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MoneyBookCategoryBudget> categories = new ArrayList<>();

    private MoneyBookBudget(MoneyBook moneyBook, int year, int month, BigDecimal totalBudget) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        this.year = year;
        this.month = month;
        changeTotalBudget(totalBudget);
    }

    public static MoneyBookBudget create(MoneyBook moneyBook, int year, int month, BigDecimal totalBudget) {
        return new MoneyBookBudget(moneyBook, year, month, totalBudget);
    }

    public void changeTotalBudget(BigDecimal totalBudget) {
        if (totalBudget != null && (totalBudget.signum() < 0 || totalBudget.scale() > 2
                || totalBudget.precision() - totalBudget.scale() > 17)) {
            throw new IllegalArgumentException("Invalid total budget");
        }
        this.totalBudget = totalBudget;
    }

    public void addCategory(MoneyBookCategory category, BigDecimal amount) {
        categories.add(MoneyBookCategoryBudget.create(this, category, amount));
    }

    public void removeCategory(MoneyBookCategoryBudget categoryBudget) {
        categories.remove(categoryBudget);
    }
}
