package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MoneyBookService {

    CreateMoneyBookResponse create(CreateMoneyBookRequest request, Authentication authentication);

    List<MoneyBookListResponse> list(Authentication authentication);
}
