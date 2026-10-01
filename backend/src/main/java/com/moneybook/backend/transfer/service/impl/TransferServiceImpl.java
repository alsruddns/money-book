package com.moneybook.backend.transfer.service.impl;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookTransfer;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.transfer.dto.CreateTransferRequest;
import com.moneybook.backend.transfer.dto.TransferResponse;
import com.moneybook.backend.transfer.dto.UpdateTransferRequest;
import com.moneybook.backend.transfer.repository.TransferRepository;
import com.moneybook.backend.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transfers;
    private final AccountRepository accounts;
    private final MoneyBookPermissionProvider permissions;

    @Override
    @Transactional
    public TransferResponse create(Long bookUid, CreateTransferRequest request, Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.CREATE);
        requireDistinct(request.fromAccountUid(), request.toAccountUid());
        validateAmount(request.amount());
        MoneyBookAccount from = account(bookUid, request.fromAccountUid());
        MoneyBookAccount to = account(bookUid, request.toAccountUid());
        return TransferResponse.from(transfers.save(MoneyBookTransfer.create(
                book, from, to, request.amount(), request.transferDate(), request.memo())));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse detail(Long bookUid, Long transferUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        return TransferResponse.from(transfer(bookUid, transferUid));
    }

    @Override
    @Transactional
    public TransferResponse update(Long bookUid, Long transferUid, UpdateTransferRequest request,
                                   Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        MoneyBookTransfer transfer = transfer(bookUid, transferUid);
        requireDistinct(request.fromAccountUid(), request.toAccountUid());
        validateAmount(request.amount());
        MoneyBookAccount from = account(bookUid, request.fromAccountUid());
        MoneyBookAccount to = account(bookUid, request.toAccountUid());
        transfer.change(from, to, request.amount(), request.transferDate(), request.memo());
        return TransferResponse.from(transfers.save(transfer));
    }

    @Override
    @Transactional
    public void delete(Long bookUid, Long transferUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.DELETE);
        transfers.delete(transfer(bookUid, transferUid));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> list(Long bookUid, int year, int month, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        YearMonth period = YearMonth.of(year, month);
        return transfers.findForPeriod(bookUid, period.atDay(1), period.plusMonths(1).atDay(1))
                .stream().map(TransferResponse::from).toList();
    }

    private MoneyBookTransfer transfer(Long bookUid, Long transferUid) {
        return transfers.findByIdAndMoneyBookUid(transferUid, bookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }

    private MoneyBookAccount account(Long bookUid, Long accountUid) {
        MoneyBookAccount account = accounts.findById(accountUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!bookUid.equals(account.getMoneyBook().getMoneyBookUid())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK);
        }
        return account;
    }

    private void requireDistinct(Long fromUid, Long toUid) {
        if (Objects.equals(fromUid, toUid)) {
            throw new BusinessException(ErrorCode.SAME_TRANSFER_ACCOUNT);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
