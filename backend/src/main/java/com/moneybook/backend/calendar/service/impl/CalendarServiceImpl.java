package com.moneybook.backend.calendar.service.impl;

import com.moneybook.backend.calendar.dto.CalendarDayResponse;
import com.moneybook.backend.calendar.dto.CalendarTransactionResponse;
import com.moneybook.backend.calendar.dto.CalendarTransferResponse;
import com.moneybook.backend.calendar.dto.DailyCalendarResponse;
import com.moneybook.backend.calendar.dto.MonthlyCalendarResponse;
import com.moneybook.backend.calendar.repository.CalendarRepository;
import com.moneybook.backend.calendar.service.CalendarService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {
    private final CalendarRepository calendar;
    private final MoneyBookPermissionProvider permissions;

    /** Exactly two grouped ledger queries cover the entire month, regardless of its day count. */
    @Override
    @Transactional(readOnly = true)
    public MonthlyCalendarResponse monthly(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        YearMonth period;
        try {
            period = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        LocalDate start = period.atDay(1);
        LocalDate end = period.plusMonths(1).atDay(1);
        Map<LocalDate, CalendarRepository.TransactionDay> transactions = calendar
                .transactionDays(bookUid, start, end).stream()
                .collect(Collectors.toMap(CalendarRepository.TransactionDay::date, Function.identity()));
        Map<LocalDate, CalendarRepository.TransferDay> transfers = calendar
                .transferDays(bookUid, start, end).stream()
                .collect(Collectors.toMap(CalendarRepository.TransferDay::date, Function.identity()));
        List<CalendarDayResponse> days = start.datesUntil(end).map(date -> {
            CalendarRepository.TransactionDay transaction = transactions.get(date);
            CalendarRepository.TransferDay transfer = transfers.get(date);
            BigDecimal transferAmount = transfer == null ? BigDecimal.ZERO : transfer.amount();
            return new CalendarDayResponse(date, date.getDayOfWeek(), weekend(date), false, null,
                    transaction == null ? BigDecimal.ZERO : transaction.income(),
                    transaction == null ? BigDecimal.ZERO : transaction.expense(),
                    transferAmount, transferAmount,
                    transaction == null ? 0 : transaction.count(), transfer == null ? 0 : transfer.count(),
                    transaction != null && transaction.recurringCount() > 0);
        }).toList();
        return new MonthlyCalendarResponse(year, month, days);
    }

    /** Two fetch-join detail queries load category and account names without per-row lookups. */
    @Override
    @Transactional(readOnly = true)
    public DailyCalendarResponse daily(Long bookUid, LocalDate date, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        if (date == null || date.getYear() < 1 || date.getYear() > 9999) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<CalendarTransactionResponse> transactions = calendar.transactionsOnDate(bookUid, date).stream()
                .map(t -> {
                    MoneyBookCategory category = t.getCategory();
                    MoneyBookAccount account = t.getAccount();
                    return new CalendarTransactionResponse(t.getTransactionUid(), t.getTransactionType(),
                            t.getAmount(), category.getCategoryUid(), category.getName(), account.getAccountUid(),
                            account.getName(), t.getMemo(), t.getRecurringTransactionUid(), t.getScheduledDate());
                }).toList();
        List<CalendarTransferResponse> transfers = calendar.transfersOnDate(bookUid, date).stream()
                .map(t -> new CalendarTransferResponse(t.getTransferUid(), t.getFromAccount().getAccountUid(),
                        t.getFromAccount().getName(), t.getToAccount().getAccountUid(),
                        t.getToAccount().getName(), t.getAmount(), t.getMemo())).toList();
        return new DailyCalendarResponse(date, false, null, transactions, transfers);
    }

    private boolean weekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
