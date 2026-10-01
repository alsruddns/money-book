package com.moneybook.backend.transaction.service;

import com.moneybook.backend.transaction.dto.CreateTransactionRequest;
import com.moneybook.backend.transaction.dto.TransactionResponse;
import com.moneybook.backend.transaction.dto.UpdateTransactionRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TransactionService {
    TransactionResponse create(Long moneyBookUid, CreateTransactionRequest request, Authentication authentication);
    TransactionResponse detail(Long moneyBookUid, Long transactionUid, Authentication authentication);
    TransactionResponse update(Long moneyBookUid, Long transactionUid, UpdateTransactionRequest request,
                               Authentication authentication);
    void delete(Long moneyBookUid, Long transactionUid, Authentication authentication);
    List<TransactionResponse> list(Long moneyBookUid, int year, int month, Authentication authentication);
}
