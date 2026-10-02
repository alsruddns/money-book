package com.moneybook.backend.transaction.service.impl;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.transaction.dto.TransactionResponse;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchResponse;
import com.moneybook.backend.transaction.repository.TransactionSearchRepository;
import com.moneybook.backend.transaction.service.TransactionSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TransactionSearchServiceImpl implements TransactionSearchService {
    private final TransactionSearchRepository repository;
    private final MoneyBookPermissionProvider permissions;

    @Override
    @Transactional(readOnly = true)
    public TransactionSearchResponse search(Long bookUid, TransactionSearchRequest request,
                                            Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        if (request.startDate() == null || request.endDate() == null
                || request.startDate().isAfter(request.endDate())
                || ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 731
                || request.page() < 0 || request.size() < 1 || request.size() > 100
                || (long) request.page() * request.size() > Integer.MAX_VALUE
                || request.sort() == null || !amountValid(request.minAmount()) || !amountValid(request.maxAmount())
                || (request.minAmount() != null && request.maxAmount() != null
                    && request.minAmount().compareTo(request.maxAmount()) > 0)
                || (request.keyword() != null && request.keyword().length() > 200)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        var result = repository.search(bookUid, request);
        long pages = result.total() / request.size() + (result.total() % request.size() == 0 ? 0 : 1);
        return new TransactionSearchResponse(result.rows().stream().map(TransactionResponse::from).toList(),
                request.page(), request.size(), result.total(), pages, request.page() == 0,
                request.page() >= pages - 1);
    }

    private boolean amountValid(java.math.BigDecimal amount) {
        return amount == null || amount.signum() >= 0 && amount.scale() <= 2
                && amount.precision() - amount.scale() <= 17;
    }
}
