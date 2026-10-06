package com.moneybook.backend.transaction.service;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** Shares date and amount filter rules between interactive search and file export. */
@Component
public class TransactionSearchValidator {
    public void validateFilters(TransactionSearchRequest request) {
        if (request.startDate() == null || request.endDate() == null
                || request.startDate().isAfter(request.endDate())
                || ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 731
                || !amountValid(request.minAmount()) || !amountValid(request.maxAmount())
                || (request.minAmount() != null && request.maxAmount() != null
                    && request.minAmount().compareTo(request.maxAmount()) > 0)
                || (request.keyword() != null && request.keyword().length() > 200)
                || (request.categoryUid() != null && request.categoryUid() < 1)
                || (request.accountUid() != null && request.accountUid() < 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public void validatePage(TransactionSearchRequest request) {
        validateFilters(request);
        if (request.page() < 0 || request.size() < 1 || request.size() > 100
                || (long) request.page() * request.size() > Integer.MAX_VALUE || request.sort() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private boolean amountValid(BigDecimal amount) {
        return amount == null || amount.signum() >= 0 && amount.scale() <= 2
                && amount.precision() - amount.scale() <= 17;
    }
}
