package com.moneybook.backend.auth.service;

import com.moneybook.backend.auth.dto.CurrentUserResponse;
import com.moneybook.backend.auth.dto.LoginRequest;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshRequest;
import com.moneybook.backend.auth.dto.RefreshResponse;
import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;
import org.springframework.security.core.Authentication;

public interface AuthService {

    SignUpResDto signUp(SignUpReqDto request);

    LoginResponse login(LoginRequest request, String userAgent, String ipAddress);

    RefreshResponse refresh(RefreshRequest request);

    void logout(Authentication authentication);

    CurrentUserResponse currentUser(Authentication authentication);
}
