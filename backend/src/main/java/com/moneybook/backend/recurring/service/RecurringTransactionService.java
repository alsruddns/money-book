package com.moneybook.backend.recurring.service;

import com.moneybook.backend.recurring.dto.CreateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionResponse;
import com.moneybook.backend.recurring.dto.RecurringTransactionResponse;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionActiveRequest;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface RecurringTransactionService {
    RecurringTransactionResponse create(Long moneyBookUid, CreateRecurringTransactionRequest request,
                                        Authentication authentication);
    List<RecurringTransactionResponse> list(Long moneyBookUid, Authentication authentication);
    RecurringTransactionResponse update(Long moneyBookUid, Long ruleUid,
                                        UpdateRecurringTransactionRequest request, Authentication authentication);
    RecurringTransactionResponse changeActive(Long moneyBookUid, Long ruleUid,
                                              UpdateRecurringTransactionActiveRequest request,
                                              Authentication authentication);
    void delete(Long moneyBookUid, Long ruleUid, Authentication authentication);
    GenerateRecurringTransactionResponse generate(Long moneyBookUid, GenerateRecurringTransactionRequest request,
                                                  Authentication authentication);
}
