package com.moneybook.backend.account.service;

import com.moneybook.backend.account.dto.AccountResponse;
import com.moneybook.backend.account.dto.CreateAccountRequest;
import com.moneybook.backend.account.dto.UpdateAccountRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AccountService {
    AccountResponse create(Long moneyBookUid, CreateAccountRequest request, Authentication authentication);
    List<AccountResponse> list(Long moneyBookUid, Authentication authentication);
    AccountResponse update(Long moneyBookUid, Long accountUid, UpdateAccountRequest request,
                           Authentication authentication);
    void delete(Long moneyBookUid, Long accountUid, Authentication authentication);
}
