package com.moneybook.backend.budget.service.impl;

import com.moneybook.backend.budget.dto.CategoryBudgetRequest;
import com.moneybook.backend.budget.dto.CategoryBudgetResponse;
import com.moneybook.backend.budget.dto.MonthlyBudgetResponse;
import com.moneybook.backend.budget.dto.SaveBudgetRequest;
import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.budget.service.BudgetService;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.closing.MonthClosingGuard;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookBudget;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookCategoryBudget;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final MoneyBookPermissionProvider permissions;
    private final MonthClosingGuard closingGuard;

    @Override
    @Transactional(readOnly = true)
    public MonthlyBudgetResponse get(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        YearMonth period = period(year, month);
        return response(bookUid, period, budgets.find(bookUid, year, month).orElse(null));
    }

    @Override
    @Transactional
    public MonthlyBudgetResponse save(Long bookUid, int year, int month, SaveBudgetRequest request,
                                      Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        YearMonth period = period(year, month);
        closingGuard.requireOpen(bookUid, List.of(period));
        if (request == null || request.categories() == null || !validAmount(request.totalBudget(), true)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Set<Long> requestedIds = new HashSet<>();
        BigDecimal categorySum = BigDecimal.ZERO;
        for (CategoryBudgetRequest item : request.categories()) {
            if (item == null || item.categoryUid() == null || !requestedIds.add(item.categoryUid())
                    || !validAmount(item.amount(), false)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            categorySum = categorySum.add(item.amount());
        }
        // A null total means "not set"; category budgets can then be managed independently.
        if (request.totalBudget() != null && categorySum.compareTo(request.totalBudget()) > 0) {
            throw new BusinessException(ErrorCode.BUDGET_CATEGORY_SUM_EXCEEDED);
        }
        Map<Long, MoneyBookCategory> validCategories = new HashMap<>();
        for (MoneyBookCategory category : categories.findAllByMoneyBookUidAndIds(bookUid, requestedIds)) {
            if (category.getTransactionType() != TransactionType.EXPENSE) {
                throw new BusinessException(ErrorCode.BUDGET_CATEGORY_NOT_EXPENSE);
            }
            validCategories.put(category.getCategoryUid(), category);
        }
        if (validCategories.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_IN_MONEY_BOOK);
        }

        MoneyBookBudget budget = budgets.find(bookUid, year, month)
                .orElseGet(() -> MoneyBookBudget.create(book, year, month, request.totalBudget()));
        budget.changeTotalBudget(request.totalBudget());
        Map<Long, MoneyBookCategoryBudget> existing = new HashMap<>();
        for (MoneyBookCategoryBudget entry : budget.getCategories()) {
            existing.put(entry.getCategory().getCategoryUid(), entry);
        }
        budget.getCategories().removeIf(entry -> !requestedIds.contains(entry.getCategory().getCategoryUid()));
        for (CategoryBudgetRequest item : request.categories()) {
            MoneyBookCategoryBudget entry = existing.get(item.categoryUid());
            if (entry == null) budget.addCategory(validCategories.get(item.categoryUid()), item.amount());
            else entry.changeAmount(item.amount());
        }
        budgets.save(budget);
        return response(bookUid, period, budget);
    }

    private MonthlyBudgetResponse response(Long bookUid, YearMonth period, MoneyBookBudget budget) {
        LocalDate start = period.atDay(1);
        LocalDate end = period.plusMonths(1).atDay(1);
        BigDecimal expense = budgets.totalExpense(bookUid, start, end);
        if (budget == null) {
            return new MonthlyBudgetResponse(false, null, bookUid, period.getYear(), period.getMonthValue(),
                    null, expense, null, null, false, List.of());
        }
        Map<Long, BigDecimal> categoryExpenses = new HashMap<>();
        for (BudgetRepository.CategoryExpense row : budgets.categoryExpenses(bookUid, start, end)) {
            categoryExpenses.put(row.categoryUid(), row.amount());
        }
        List<CategoryBudgetResponse> rows = budget.getCategories().stream()
                .map(item -> {
                    BigDecimal spent = categoryExpenses.getOrDefault(item.getCategory().getCategoryUid(), BigDecimal.ZERO);
                    BigDecimal amount = item.getAmount();
                    return new CategoryBudgetResponse(item.getCategory().getCategoryUid(), item.getCategory().getName(),
                            amount, spent, amount.subtract(spent), rate(spent, amount), spent.compareTo(amount) > 0);
                })
                .sorted((a, b) -> a.categoryUid().compareTo(b.categoryUid())).toList();
        BigDecimal total = budget.getTotalBudget();
        return new MonthlyBudgetResponse(true, budget.getBudgetUid(), bookUid, period.getYear(),
                period.getMonthValue(), total, expense, total == null ? null : total.subtract(expense),
                rate(expense, total), total != null && expense.compareTo(total) > 0, rows);
    }

    private BigDecimal rate(BigDecimal spent, BigDecimal budget) {
        if (budget == null) return null;
        if (budget.signum() == 0) return BigDecimal.ZERO;
        return spent.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);
    }

    private YearMonth period(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private boolean validAmount(BigDecimal amount, boolean nullable) {
        return amount == null ? nullable : amount.signum() >= 0 && amount.scale() <= 2
                && amount.precision() - amount.scale() <= 17;
    }
}
