package com.moneybook.backend.dashboard.service.impl;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.dashboard.dto.AccountSummaryResponse;
import com.moneybook.backend.dashboard.dto.CategorySummaryResponse;
import com.moneybook.backend.dashboard.dto.MonthlyDashboardResponse;
import com.moneybook.backend.dashboard.repository.DashboardRepository;
import com.moneybook.backend.dashboard.service.DashboardService;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardRepository dashboard;
    private final AccountRepository accountRepository;
    private final MoneyBookPermissionProvider permissions;

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
