package com.moneybook.backend.recurring.service.impl;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.RecurringTransaction;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.recurring.dto.CreateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionResponse;
import com.moneybook.backend.recurring.dto.RecurringTransactionResponse;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionActiveRequest;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionRequest;
import com.moneybook.backend.recurring.repository.RecurringTransactionRepository;
import com.moneybook.backend.recurring.service.RecurringTransactionService;
import com.moneybook.backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {
    private static final int MAX_GENERATED_PER_REQUEST = 500;

    private final RecurringTransactionRepository rules;
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final AccountRepository accounts;
    private final MoneyBookPermissionProvider permissions;

    @Override
    @Transactional
    public RecurringTransactionResponse create(Long bookUid, CreateRecurringTransactionRequest request,
                                               Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.CREATE);
        validate(request.transactionType(), request.amount(), request.frequency(), request.dayOfMonth(),
                request.dayOfWeek(), request.startDate(), request.endDate());
        MoneyBookCategory category = category(bookUid, request.categoryUid(), request.transactionType());
        MoneyBookAccount account = account(bookUid, request.accountUid());
        return RecurringTransactionResponse.from(rules.save(RecurringTransaction.create(book,
                request.transactionType(), request.amount(), category, account, request.frequency(),
                request.dayOfMonth(), request.dayOfWeek(), request.startDate(), request.endDate(), request.memo())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> list(Long bookUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        return rules.findByMoneyBookUid(bookUid).stream().map(RecurringTransactionResponse::from).toList();
    }

    @Override
    @Transactional
    public RecurringTransactionResponse update(Long bookUid, Long ruleUid,
                                               UpdateRecurringTransactionRequest request,
                                               Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        RecurringTransaction rule = rule(bookUid, ruleUid);
        validate(request.transactionType(), request.amount(), request.frequency(), request.dayOfMonth(),
                request.dayOfWeek(), request.startDate(), request.endDate());
        MoneyBookCategory category = category(bookUid, request.categoryUid(), request.transactionType());
        MoneyBookAccount account = account(bookUid, request.accountUid());
        rule.change(request.transactionType(), request.amount(), category, account, request.frequency(),
                request.dayOfMonth(), request.dayOfWeek(), request.startDate(), request.endDate(), request.memo());
        return RecurringTransactionResponse.from(rules.save(rule));
    }

    @Override
    @Transactional
    public RecurringTransactionResponse changeActive(Long bookUid, Long ruleUid,
                                                     UpdateRecurringTransactionActiveRequest request,
                                                     Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        RecurringTransaction rule = rule(bookUid, ruleUid);
        rule.changeActive(request.active());
        return RecurringTransactionResponse.from(rules.save(rule));
    }

    @Override
    @Transactional
    public void delete(Long bookUid, Long ruleUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.DELETE);
        rules.delete(rule(bookUid, ruleUid));
    }

    /** Locks each rule before checking occurrence rows; all generated transactions commit or roll back together. */
    @Override
    @Transactional
    public GenerateRecurringTransactionResponse generate(Long bookUid, GenerateRecurringTransactionRequest request,
                                                         Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.CREATE);
        LocalDate baseDate = request.baseDate();
        if (baseDate == null || baseDate.getYear() < 1 || baseDate.getYear() > 9999) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<PendingOccurrence> pending = new ArrayList<>();
        for (Long ruleUid : rules.findActiveIdsDueBy(bookUid, baseDate)) {
            RecurringTransaction rule = rules.lockById(ruleUid).orElse(null);
            if (rule == null || !rule.isActive() || !bookUid.equals(rule.getMoneyBook().getMoneyBookUid())
                    || rule.getStartDate().isAfter(baseDate)) {
                continue;
            }
            LocalDate through = rule.getEndDate() == null || rule.getEndDate().isAfter(baseDate)
                    ? baseDate : rule.getEndDate();
            if (through.isBefore(rule.getStartDate())) {
                continue;
            }
            Set<LocalDate> existing = new HashSet<>(
                    transactions.findGeneratedDates(ruleUid, through));
            collectPending(rule, through, existing, pending);
        }
        for (PendingOccurrence occurrence : pending) {
            RecurringTransaction rule = occurrence.rule();
            transactions.save(MoneyBookTransaction.createRecurring(rule.getMoneyBook(), rule.getTransactionType(),
                    rule.getAmount(), occurrence.date(), rule.getCategory(), rule.getAccount(),
                    rule.getMemo(), rule.getRecurringTransactionUid()));
            rule.recordGeneratedThrough(occurrence.date());
        }
        return new GenerateRecurringTransactionResponse(baseDate, pending.size());
    }

    /** Monthly dates clamp to the last day; weekly days use ISO 1=Monday through 7=Sunday. */
    private void collectPending(RecurringTransaction rule, LocalDate through,
                                Set<LocalDate> existing, List<PendingOccurrence> pending) {
        if (rule.getFrequency() == RecurringFrequency.MONTHLY) {
            for (YearMonth month = YearMonth.from(rule.getStartDate());
                 !month.atDay(1).isAfter(through); month = month.plusMonths(1)) {
                LocalDate date = month.atDay(Math.min(rule.getDayOfMonth(), month.lengthOfMonth()));
                addIfMissing(rule, date, through, existing, pending);
            }
        } else {
            LocalDate first = rule.getStartDate().with(TemporalAdjusters.nextOrSame(
                    DayOfWeek.of(rule.getDayOfWeek())));
            for (LocalDate date = first; !date.isAfter(through); date = date.plusWeeks(1)) {
                addIfMissing(rule, date, through, existing, pending);
            }
        }
    }

    private void addIfMissing(RecurringTransaction rule, LocalDate date, LocalDate through,
                              Set<LocalDate> existing, List<PendingOccurrence> pending) {
        if (date.isBefore(rule.getStartDate()) || date.isAfter(through) || existing.contains(date)) {
            return;
        }
        if (pending.size() >= MAX_GENERATED_PER_REQUEST) {
            throw new BusinessException(ErrorCode.RECURRING_GENERATION_LIMIT_EXCEEDED);
        }
        pending.add(new PendingOccurrence(rule, date));
    }

    private RecurringTransaction rule(Long bookUid, Long ruleUid) {
        return rules.findByIdAndMoneyBookUid(ruleUid, bookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRING_TRANSACTION_NOT_FOUND));
    }

    private MoneyBookCategory category(Long bookUid, Long categoryUid, TransactionType type) {
        MoneyBookCategory category = categories.findById(categoryUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!bookUid.equals(category.getMoneyBook().getMoneyBookUid())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_IN_MONEY_BOOK);
        }
        if (category.getTransactionType() != type) {
            throw new BusinessException(ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH);
        }
        return category;
    }

    private MoneyBookAccount account(Long bookUid, Long accountUid) {
        MoneyBookAccount account = accounts.findById(accountUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!bookUid.equals(account.getMoneyBook().getMoneyBookUid())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK);
        }
        return account;
    }

    private void validate(TransactionType type, BigDecimal amount, RecurringFrequency frequency,
                          Integer dayOfMonth, Integer dayOfWeek, LocalDate startDate, LocalDate endDate) {
        if (type == null || amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17 || frequency == null || startDate == null
                || startDate.getYear() < 1 || startDate.getYear() > 9999
                || (endDate != null && (endDate.getYear() < 1 || endDate.getYear() > 9999))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_RECURRING_DATE_RANGE);
        }
        if ((frequency == RecurringFrequency.MONTHLY
                && (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31 || dayOfWeek != null))
                || (frequency == RecurringFrequency.WEEKLY
                && (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7 || dayOfMonth != null))) {
            throw new BusinessException(ErrorCode.INVALID_RECURRING_DAY);
        }
    }

    private record PendingOccurrence(RecurringTransaction rule, LocalDate date) { }
}
