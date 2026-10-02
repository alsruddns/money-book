package com.moneybook.backend.export.service.impl;

import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.export.service.ExportService;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.service.TransactionSearchValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {
    private final TransactionSearchValidator validator;
    private final MoneyBookPermissionProvider permissions;
    private final TransactionFileWriter writer;

    @Override
    @Transactional(readOnly = true)
    public void validate(Long bookUid, TransactionSearchRequest filters, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        validator.validateFilters(filters);
    }

    @Override
    @Transactional(readOnly = true)
    public long writeCsv(Long bookUid, TransactionSearchRequest filters, Authentication authentication,
                         OutputStream output) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        validator.validateFilters(filters);
        return writer.csv(bookUid, filters, output);
    }

    @Override
    @Transactional(readOnly = true)
    public long writeXlsx(Long bookUid, TransactionSearchRequest filters, Authentication authentication,
                          OutputStream output) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        validator.validateFilters(filters);
        return writer.xlsx(bookUid, filters, output);
    }
}
