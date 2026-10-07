package com.moneybook.backend.dashboard.service.impl;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.dashboard.dto.AccountSummaryResponse;
import com.moneybook.backend.dashboard.dto.CategorySummaryResponse;
import com.moneybook.backend.dashboard.dto.MonthlyDashboardResponse;
import com.moneybook.backend.dashboard.dto.DashboardResponse;
import com.moneybook.backend.dashboard.repository.DashboardRepository;
import com.moneybook.backend.dashboard.service.DashboardService;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardRepository dashboard;
    private final AccountRepository accountRepository;
    private final MoneyBookPermissionProvider permissions;
    private final BudgetRepository budgets;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        YearMonth period = period(year, month);
        LocalDate start = period.atDay(1);
        LocalDate end = period.plusMonths(1).atDay(1);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        long incomeCount = 0;
        long expenseCount = 0;
        for (var row : dashboard.transactionTotals(bookUid, start, end)) {
            if (row.type() == TransactionType.INCOME) {
                income = row.amount();
                incomeCount = row.count();
            } else {
                expense = row.amount();
                expenseCount = row.count();
            }
        }

        var categoryRows = dashboard.categoryTotals(bookUid, start, end, TransactionType.EXPENSE);
        BigDecimal categoryTotal = categoryRows.stream().map(DashboardRepository.CategoryTotal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var categories = categoryRows.stream().map(row -> new DashboardResponse.CategoryExpense(
                row.uid(), row.name(), row.amount(), row.count(), categoryTotal.signum() == 0
                ? BigDecimal.ZERO : row.amount().divide(categoryTotal, 4, RoundingMode.HALF_UP))).toList();

        // A single bounded aggregate query supplies six consecutive buckets, including empty months.
        YearMonth firstTrendMonth = period.minusMonths(5);
        Map<YearMonth, DashboardRepository.MonthTotal> trendRows = new HashMap<>();
        for (var row : dashboard.monthlyTotals(bookUid, firstTrendMonth.atDay(1), end)) {
            trendRows.put(YearMonth.of(row.year(), row.month()), row);
        }
        List<DashboardResponse.MonthTrend> trend = java.util.stream.LongStream.rangeClosed(0, 5)
                .mapToObj(offset -> firstTrendMonth.plusMonths(offset))
                .map(monthPeriod -> {
                    var row = trendRows.get(monthPeriod);
                    BigDecimal monthIncome = row == null ? BigDecimal.ZERO : row.income();
                    BigDecimal monthExpense = row == null ? BigDecimal.ZERO : row.expense();
                    return new DashboardResponse.MonthTrend(monthPeriod.getYear(), monthPeriod.getMonthValue(),
                            monthIncome, monthExpense, monthIncome.subtract(monthExpense));
                }).toList();

        var previous = trendRows.get(period.minusMonths(1));
        BigDecimal previousIncome = previous == null ? BigDecimal.ZERO : previous.income();
        BigDecimal previousExpense = previous == null ? BigDecimal.ZERO : previous.expense();
        var savedBudget = budgets.find(bookUid, year, month).orElse(null);
        DashboardResponse.BudgetSummary budget = null;
        if (savedBudget != null) {
            BigDecimal limit = savedBudget.getTotalBudget();
            BigDecimal remaining = limit == null ? null : limit.subtract(expense);
            BigDecimal usage = limit == null ? null : limit.signum() == 0 ? BigDecimal.ZERO
                    : expense.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);
            budget = new DashboardResponse.BudgetSummary(limit, expense, remaining, usage,
                    limit != null && expense.compareTo(limit) > 0);
        }

        var topRows = dashboard.topExpenses(bookUid, start, end, 5);
        List<DashboardResponse.TopExpense> top = java.util.stream.IntStream.range(0, topRows.size())
                .mapToObj(index -> {
                    var row = topRows.get(index);
                    return new DashboardResponse.TopExpense(index + 1, row.transactionUid(), row.date(),
                            row.categoryUid(), row.categoryName(), row.accountUid(), row.accountName(),
                            row.memo(), row.amount());
                }).toList();

        return new DashboardResponse(year, month,
                new DashboardResponse.Summary(income, expense, income.subtract(expense),
                        incomeCount + expenseCount, incomeCount, expenseCount),
                new DashboardResponse.Comparison(previousIncome, previousExpense,
                        changeRate(income, previousIncome), changeRate(expense, previousExpense)),
                categories, trend, budget, top);
    }

    private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
        BigDecimal change = current.subtract(previous);
        if (previous.signum() == 0) return change.signum() == 0 ? BigDecimal.ZERO : null;
        return change.multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private YearMonth period(int year, int month) {
        try {
            if (year < 2 || year >= 9999) throw new java.time.DateTimeException("Year outside dashboard range");
            return YearMonth.of(year, month);
        } catch (java.time.DateTimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyDashboardResponse monthly(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        LocalDate start = monthStart(year, month);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        long incomeCount = 0;
        long expenseCount = 0;
        // Transfers have their own table, so they never enter income, expense or balance.
        for (DashboardRepository.TypeTotal total : dashboard.transactionTotals(bookUid, start, start.plusMonths(1))) {
            if (total.type() == TransactionType.INCOME) {
                income = total.amount();
                incomeCount = total.count();
            } else {
                expense = total.amount();
                expenseCount = total.count();
            }
        }
        return new MonthlyDashboardResponse(year, month, income, expense, income.subtract(expense),
                incomeCount + expenseCount, incomeCount, expenseCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> categories(Long bookUid, int year, int month,
                                                    TransactionType type, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        LocalDate start = monthStart(year, month);
        if (type == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<DashboardRepository.CategoryTotal> rows = dashboard.categoryTotals(
                bookUid, start, start.plusMonths(1), type);
        BigDecimal overall = rows.stream().map(DashboardRepository.CategoryTotal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal denominator = overall;
        return rows.stream().map(row -> new CategorySummaryResponse(row.uid(), row.name(), row.type(),
                row.amount(), row.count(), denominator.signum() == 0 ? BigDecimal.ZERO
                : row.amount().divide(denominator, 4, RoundingMode.HALF_UP))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> accounts(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        LocalDate start = monthStart(year, month);
        LocalDate end = start.plusMonths(1);
        Map<Long, BigDecimal> income = new HashMap<>();
        Map<Long, BigDecimal> expense = new HashMap<>();
        for (DashboardRepository.AccountTransactionTotal total :
                dashboard.accountTransactionTotals(bookUid, start, end)) {
            (total.type() == TransactionType.INCOME ? income : expense).put(total.uid(), total.amount());
        }
        Map<Long, BigDecimal> transferIn = new HashMap<>();
        Map<Long, BigDecimal> transferOut = new HashMap<>();
        for (DashboardRepository.AccountTransferTotal total : dashboard.transferInTotals(bookUid, start, end)) {
            transferIn.put(total.uid(), total.amount());
        }
        for (DashboardRepository.AccountTransferTotal total : dashboard.transferOutTotals(bookUid, start, end)) {
            transferOut.put(total.uid(), total.amount());
        }
        // One account query includes accounts with no monthly activity; aggregate maps cost no row-level queries.
        return accountRepository.findByMoneyBookUid(bookUid).stream().map(account -> response(
                account, income, expense, transferIn, transferOut)).toList();
    }

    private AccountSummaryResponse response(MoneyBookAccount account, Map<Long, BigDecimal> income,
                                            Map<Long, BigDecimal> expense, Map<Long, BigDecimal> transferIn,
                                            Map<Long, BigDecimal> transferOut) {
        Long uid = account.getAccountUid();
        return new AccountSummaryResponse(uid, account.getName(), income.getOrDefault(uid, BigDecimal.ZERO),
                expense.getOrDefault(uid, BigDecimal.ZERO), transferIn.getOrDefault(uid, BigDecimal.ZERO),
                transferOut.getOrDefault(uid, BigDecimal.ZERO));
    }

    private LocalDate monthStart(int year, int month) {
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return LocalDate.of(year, month, 1);
    }
}
