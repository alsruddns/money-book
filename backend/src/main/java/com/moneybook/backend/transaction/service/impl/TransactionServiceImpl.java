package com.moneybook.backend.transaction.service.impl;

import com.moneybook.backend.activity.ActivityRecorder;
import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.closing.MonthClosingGuard;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.transaction.dto.CreateTransactionRequest;
import com.moneybook.backend.transaction.dto.TransactionResponse;
import com.moneybook.backend.transaction.dto.UpdateTransactionRequest;
import com.moneybook.backend.transaction.repository.TransactionRepository;
import com.moneybook.backend.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final AccountRepository accounts;
    private final MoneyBookPermissionProvider permissions;
    private final MonthClosingGuard closingGuard;
    private final ActivityRecorder activityRecorder;

    @Override
    @Transactional
    public TransactionResponse create(Long bookUid, CreateTransactionRequest request, Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.CREATE);
        closingGuard.requireOpen(bookUid, request.transactionDate());
        validateAmount(request.amount());
        MoneyBookCategory category = category(bookUid, request.categoryUid(), request.transactionType());
        MoneyBookAccount account = account(bookUid, request.accountUid());
        var saved = transactions.save(MoneyBookTransaction.create(
                book, request.transactionType(), request.amount(), request.transactionDate(),
                category, account, request.memo()));
        activityRecorder.record(bookUid, authentication, ActivityType.TRANSACTION_CREATED,
                ActivityTargetType.TRANSACTION, saved.getTransactionUid(), "거래를 등록했습니다.", null);
        return TransactionResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse detail(Long bookUid, Long transactionUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        return TransactionResponse.from(transaction(bookUid, transactionUid));
    }

    @Override
    @Transactional
    public TransactionResponse update(Long bookUid, Long transactionUid, UpdateTransactionRequest request,
                                      Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        MoneyBookTransaction entry = transaction(bookUid, transactionUid);
        closingGuard.requireOpen(bookUid, List.of(YearMonth.from(entry.getTransactionDate()),
                YearMonth.from(request.transactionDate())));
        validateAmount(request.amount());
        MoneyBookCategory category = category(bookUid, request.categoryUid(), request.transactionType());
        MoneyBookAccount account = account(bookUid, request.accountUid());
        entry.change(request.transactionType(), request.amount(), request.transactionDate(),
                category, account, request.memo());
        activityRecorder.record(bookUid, authentication, ActivityType.TRANSACTION_UPDATED,
                ActivityTargetType.TRANSACTION, transactionUid, "거래를 수정했습니다.", null);
        return TransactionResponse.from(transactions.save(entry));
    }

    @Override
    @Transactional
    public void delete(Long bookUid, Long transactionUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.DELETE);
        MoneyBookTransaction entry = transaction(bookUid, transactionUid);
        closingGuard.requireOpen(bookUid, entry.getTransactionDate());
        transactions.delete(entry);
        activityRecorder.record(bookUid, authentication, ActivityType.TRANSACTION_DELETED,
                ActivityTargetType.TRANSACTION, transactionUid, "거래를 삭제했습니다.", null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        YearMonth period = YearMonth.of(year, month);
        return transactions.findForPeriod(bookUid, period.atDay(1), period.plusMonths(1).atDay(1))
                .stream().map(TransactionResponse::from).toList();
    }

    private MoneyBookTransaction transaction(Long bookUid, Long transactionUid) {
        return transactions.findByIdAndMoneyBookUid(transactionUid, bookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    /** Resolves the ID first so a foreign book and a missing category produce distinct safe errors. */
    private MoneyBookCategory category(Long bookUid, Long categoryUid, TransactionType type) {
        MoneyBookCategory category = categories.findById(categoryUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!category.getMoneyBook().getMoneyBookUid().equals(bookUid)) {
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
        if (!account.getMoneyBook().getMoneyBookUid().equals(bookUid)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK);
        }
        return account;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
