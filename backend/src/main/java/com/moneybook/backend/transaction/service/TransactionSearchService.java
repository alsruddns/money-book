package com.moneybook.backend.transaction.service;

import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchResponse;
import org.springframework.security.core.Authentication;

public interface TransactionSearchService {
    TransactionSearchResponse search(Long bookUid, TransactionSearchRequest request, Authentication authentication);
}
