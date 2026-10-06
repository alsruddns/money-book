package com.moneybook.backend.accountmanagement.service;

import com.moneybook.backend.accountmanagement.dto.AccountMeResDto;
import com.moneybook.backend.accountmanagement.dto.AccountPasswordUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountProfileUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountWithdrawalRequest;
import org.springframework.security.core.Authentication;

public interface AccountManagementService {

    AccountMeResDto me(Authentication authentication);

    AccountMeResDto updateProfile(Authentication authentication, AccountProfileUpdateReqDto request);

    AccountMeResDto updatePassword(Authentication authentication, AccountPasswordUpdateReqDto request);

    void withdraw(Authentication authentication, AccountWithdrawalRequest request);
}
