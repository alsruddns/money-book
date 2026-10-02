package com.moneybook.backend.account.service.impl;

import com.moneybook.backend.activity.ActivityRecorder;
import com.moneybook.backend.account.dto.AccountResponse;
import com.moneybook.backend.account.dto.CreateAccountRequest;
import com.moneybook.backend.account.dto.UpdateAccountRequest;
import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.account.service.AccountService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accounts;
    private final MoneyBookPermissionProvider permissions;
    private final ActivityRecorder activityRecorder;

    @Override
    @Transactional
    public AccountResponse create(Long bookUid, CreateAccountRequest request, Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.CREATE);
        if (accounts.existsByName(bookUid, request.name(), null)) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        try {
            var saved = accounts.save(MoneyBookAccount.create(book, request.name(), request.accountType(), request.sortOrder()));
            activityRecorder.record(bookUid, authentication, ActivityType.ACCOUNT_CREATED, ActivityTargetType.ACCOUNT,
                    saved.getAccountUid(), "계좌 '" + saved.getName() + "'를 만들었습니다.", null);
            return AccountResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> list(Long bookUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        return accounts.findByMoneyBookUid(bookUid).stream().map(AccountResponse::from).toList();
    }

    @Override
    @Transactional
    public AccountResponse update(Long bookUid, Long accountUid, UpdateAccountRequest request,
                                  Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        MoneyBookAccount account = account(bookUid, accountUid);
        if (accounts.existsByName(bookUid, request.name(), accountUid)) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        account.change(request.name(), request.accountType(), request.sortOrder());
        try {
            accounts.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        activityRecorder.record(bookUid, authentication, ActivityType.ACCOUNT_UPDATED, ActivityTargetType.ACCOUNT,
                accountUid, "계좌 '" + account.getName() + "'를 수정했습니다.", null);
        return AccountResponse.from(account);
    }

    @Override
    @Transactional
    public void delete(Long bookUid, Long accountUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.DELETE);
        MoneyBookAccount account = account(bookUid, accountUid);
        if (accounts.isInUse(accountUid)) {
            throw new BusinessException(ErrorCode.ACCOUNT_IN_USE);
        }
        try {
            accounts.delete(account);
            accounts.flush();
            activityRecorder.record(bookUid, authentication, ActivityType.ACCOUNT_DELETED, ActivityTargetType.ACCOUNT,
                    accountUid, "계좌 '" + account.getName() + "'를 삭제했습니다.", null);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_IN_USE);
        }
    }

    private MoneyBookAccount account(Long bookUid, Long accountUid) {
        return accounts.findByIdAndMoneyBookUid(accountUid, bookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
