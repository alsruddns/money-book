package com.moneybook.backend.closing.service.impl;

import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.closing.dto.MonthClosingResponse;
import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.closing.service.MonthClosingService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.dashboard.repository.DashboardRepository;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookMonthClosing;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class MonthClosingServiceImpl implements MonthClosingService {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final MonthClosingRepository closings;
    private final DashboardRepository dashboard;
    private final BudgetRepository budgets;
    private final MoneyBookPermissionProvider permissions;

    /** Locks the book before capturing totals, so concurrent ledger writes cannot pass the closed-month guard. */
    @Override
    @Transactional
    public MonthClosingResponse close(Long bookUid, int year, int month, Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        YearMonth period = period(year, month);
        if (period.isAfter(YearMonth.now(KOREA))) throw new BusinessException(ErrorCode.FUTURE_MONTH_CLOSING);
        closings.lockBook(bookUid);
        if (closings.exists(bookUid, year, month)) throw new BusinessException(ErrorCode.MONTH_ALREADY_CLOSED);
        Totals current = totals(bookUid, period);
        Totals previous = totals(bookUid, period.minusMonths(1));
        var budget = budgets.find(bookUid, year, month).orElse(null);
        MoneyBookMonthClosing closing = MoneyBookMonthClosing.create(book, year, month,
                current.income(), current.expense(), current.count(), previous.income(), previous.expense(),
                budget != null, budget == null ? null : budget.getTotalBudget(),
                Long.parseLong(authentication.getName()), LocalDateTime.now(KOREA));
        return MonthClosingResponse.from(closings.save(closing));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthClosingResponse get(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        period(year, month);
        return MonthClosingResponse.from(closings.find(bookUid, year, month)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONTH_NOT_CLOSED)));
    }

    @Override
    @Transactional
    public void cancel(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        period(year, month);
        closings.lockBook(bookUid);
        var closing = closings.find(bookUid, year, month)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONTH_NOT_CLOSED));
        closings.delete(closing);
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

    private YearMonth period(int year, int month) {
        try {
            if (year < 2 || year > 9999) throw new DateTimeException("Year outside closing range");
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private record Totals(BigDecimal income, BigDecimal expense, long count) { }
}
