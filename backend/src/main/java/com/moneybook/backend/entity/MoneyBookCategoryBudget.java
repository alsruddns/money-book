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
import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_category_budgets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookCategoryBudget extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_budget_uid")
    private Long categoryBudgetUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_uid", nullable = false)
    private MoneyBookBudget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_uid", nullable = false)
    private MoneyBookCategory category;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private MoneyBookCategoryBudget(MoneyBookBudget budget, MoneyBookCategory category, BigDecimal amount) {
        this.budget = Objects.requireNonNull(budget, "budget");
        this.category = Objects.requireNonNull(category, "category");
        changeAmount(amount);
    }

    public static MoneyBookCategoryBudget create(MoneyBookBudget budget, MoneyBookCategory category, BigDecimal amount) {
        return new MoneyBookCategoryBudget(budget, category, amount);
    }

    public void changeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Invalid category budget amount");
        }
        this.amount = amount;
    }
}
