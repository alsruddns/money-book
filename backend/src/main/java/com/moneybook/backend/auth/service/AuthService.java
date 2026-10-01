package com.moneybook.backend.auth.service;

import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;

public interface AuthService {

    SignUpResDto signUp(SignUpReqDto request);
}
