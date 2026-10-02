package com.moneybook.backend.report.service.impl;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.closing.dto.MonthClosingResponse;
import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.dashboard.repository.DashboardRepository;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.report.dto.AccountStatisticsResponse;
import com.moneybook.backend.report.dto.CategoryStatisticsResponse;
import com.moneybook.backend.report.dto.MonthlyReportResponse;
import com.moneybook.backend.report.dto.YearlyMonthResponse;
import com.moneybook.backend.report.dto.YearlyReportResponse;
import com.moneybook.backend.report.repository.ReportRepository;
import com.moneybook.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reports;
    private final DashboardRepository dashboard;
    private final BudgetRepository budgets;
    private final MonthClosingRepository closings;
    private final AccountRepository accounts;
    private final MoneyBookPermissionProvider permissions;

    @Override
    @Transactional(readOnly = true)
    public YearlyReportResponse yearly(Long bookUid, int year, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        if (year < 1 || year >= 9999) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        Map<Integer, ReportRepository.MonthTotal> byMonth = new HashMap<>();
        for (var row : reports.monthlyTotals(bookUid, LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1))) {
            byMonth.put(row.month(), row);
        }
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        List<YearlyMonthResponse> months = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            var row = byMonth.get(month);
            BigDecimal monthIncome = row == null ? BigDecimal.ZERO : row.income();
            BigDecimal monthExpense = row == null ? BigDecimal.ZERO : row.expense();
            months.add(new YearlyMonthResponse(month, monthIncome, monthExpense,
                    monthIncome.subtract(monthExpense), row == null ? 0 : row.count()));
            income = income.add(monthIncome);
            expense = expense.add(monthExpense);
        }
        return new YearlyReportResponse(year, income, expense, income.subtract(expense), months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryStatisticsResponse> categories(Long bookUid, LocalDate start, LocalDate end,
                                                       TransactionType type, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        LocalDate endExclusive = endExclusive(start, end);
        if (type == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        var rows = dashboard.categoryTotals(bookUid, start, endExclusive, type);
        BigDecimal total = rows.stream().map(DashboardRepository.CategoryTotal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream().map(row -> new CategoryStatisticsResponse(row.uid(), row.name(), row.type(),
                row.amount(), row.count(), total.signum() == 0 ? BigDecimal.ZERO
                : row.amount().divide(total, 4, RoundingMode.HALF_UP))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountStatisticsResponse> accounts(Long bookUid, LocalDate start, LocalDate end,
                                                    Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        LocalDate endExclusive = endExclusive(start, end);
        Map<Long, BigDecimal> income = new HashMap<>();
        Map<Long, BigDecimal> expense = new HashMap<>();
        for (var row : dashboard.accountTransactionTotals(bookUid, start, endExclusive)) {
            (row.type() == TransactionType.INCOME ? income : expense).put(row.uid(), row.amount());
        }
        Map<Long, BigDecimal> transferIn = new HashMap<>();
        Map<Long, BigDecimal> transferOut = new HashMap<>();
        for (var row : dashboard.transferInTotals(bookUid, start, endExclusive)) transferIn.put(row.uid(), row.amount());
        for (var row : dashboard.transferOutTotals(bookUid, start, endExclusive)) transferOut.put(row.uid(), row.amount());
        return accounts.findByMoneyBookUid(bookUid).stream().map(account -> {
            Long uid = account.getAccountUid();
            BigDecimal in = income.getOrDefault(uid, BigDecimal.ZERO);
            BigDecimal out = expense.getOrDefault(uid, BigDecimal.ZERO);
            BigDecimal movedIn = transferIn.getOrDefault(uid, BigDecimal.ZERO);
            BigDecimal movedOut = transferOut.getOrDefault(uid, BigDecimal.ZERO);
            return new AccountStatisticsResponse(uid, account.getName(), in, out, movedIn, movedOut,
                    in.subtract(out).add(movedIn).subtract(movedOut));
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        YearMonth period = period(year, month);
        var closed = closings.find(bookUid, year, month).orElse(null);
        if (closed != null) {
            MonthClosingResponse snapshot = MonthClosingResponse.from(closed);
            return new MonthlyReportResponse(year, month, snapshot.income(), snapshot.expense(),
                    snapshot.balance(), snapshot.transactionCount(), snapshot.previousIncome(),
                    snapshot.previousExpense(), snapshot.incomeChange(), snapshot.expenseChange(),
                    snapshot.incomeChangeRate(), snapshot.expenseChangeRate(), snapshot.budgetConfigured(),
                    snapshot.totalBudget(), snapshot.remainingBudget(), snapshot.budgetUsageRate(),
                    snapshot.overBudget());
        }
        Totals current = totals(bookUid, period);
        Totals previous = totals(bookUid, period.minusMonths(1));
        BigDecimal incomeChange = current.income().subtract(previous.income());
        BigDecimal expenseChange = current.expense().subtract(previous.expense());
        var budget = budgets.find(bookUid, year, month).orElse(null);
        BigDecimal limit = budget == null ? null : budget.getTotalBudget();
        return new MonthlyReportResponse(year, month, current.income(), current.expense(),
                current.income().subtract(current.expense()), current.count(), previous.income(), previous.expense(),
                incomeChange, expenseChange, changeRate(incomeChange, previous.income()),
                changeRate(expenseChange, previous.expense()), budget != null, limit,
                limit == null ? null : limit.subtract(current.expense()),
                limit == null ? null : limit.signum() == 0 ? BigDecimal.ZERO
                        : current.expense().multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP),
                limit != null && current.expense().compareTo(limit) > 0);
    }

    private Totals totals(Long bookUid, YearMonth period) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        long count = 0;
        for (var row : dashboard.transactionTotals(bookUid, period.atDay(1), period.plusMonths(1).atDay(1))) {
            if (row.type() == TransactionType.INCOME) income = row.amount();
            else expense = row.amount();
            count += row.count();
        }
        return new Totals(income, expense, count);
    }

    private BigDecimal changeRate(BigDecimal change, BigDecimal previous) {
        if (previous.signum() == 0) return change.signum() == 0 ? BigDecimal.ZERO : null;
        return change.multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private LocalDate endExclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)
                || ChronoUnit.DAYS.between(start, end) > 731 || end.equals(LocalDate.MAX)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return end.plusDays(1);
    }

    private YearMonth period(int year, int month) {
        try {
            if (year < 2 || year >= 9999) throw new DateTimeException("Year outside report range");
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private record Totals(BigDecimal income, BigDecimal expense, long count) { }
}
