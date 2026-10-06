package com.moneybook.backend.transaction.service.impl;

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


@Service
@RequiredArgsConstructor
public class TransactionSearchServiceImpl implements TransactionSearchService {
    private final TransactionSearchRepository repository;
    private final MoneyBookPermissionProvider permissions;
    private final com.moneybook.backend.transaction.service.TransactionSearchValidator validator;

    @Override
    @Transactional(readOnly = true)
    public TransactionSearchResponse search(Long bookUid, TransactionSearchRequest request,
                                            Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        validator.validatePage(request);
        var result = repository.search(bookUid, request);
        long pages = result.total() / request.size() + (result.total() % request.size() == 0 ? 0 : 1);
        return new TransactionSearchResponse(result.rows().stream().map(TransactionResponse::from).toList(),
                request.page(), request.size(), result.total(), pages, request.page() == 0,
                request.page() >= pages - 1);
    }

}
