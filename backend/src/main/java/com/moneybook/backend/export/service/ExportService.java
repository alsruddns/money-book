package com.moneybook.backend.export.service;

import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import org.springframework.security.core.Authentication;
import java.io.OutputStream;

public interface ExportService {
    void validate(Long bookUid, TransactionSearchRequest filters, Authentication authentication);
    long writeCsv(Long bookUid, TransactionSearchRequest filters, Authentication authentication, OutputStream output);
    long writeXlsx(Long bookUid, TransactionSearchRequest filters, Authentication authentication, OutputStream output);
}
