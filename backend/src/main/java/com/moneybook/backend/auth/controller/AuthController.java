package com.moneybook.backend.auth.controller;

import com.moneybook.backend.auth.dto.LoginRequest;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LOCAL 회원가입과 로그인 API를 제공한다.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 인증 없이 LOCAL 계정을 생성한다. 비밀번호 확인 후 암호화된 인증 정보와 사용자를 함께 저장한다.
     *
     * @param request 로그인 ID, 비밀번호, 비밀번호 확인 값과 닉네임
     * @return 생성된 사용자 UID와 닉네임
     */
    @PostMapping("/signup")
    public ResponseEntity<SignUpResDto> signUp(@Valid @RequestBody SignUpReqDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    /**
     * 인증 없이 LOCAL 계정의 로그인 ID와 비밀번호를 검증하고 Access Token을 발급한다.
     *
     * @param request 로그인 ID와 비밀번호
     * @return 사용자 UID, 닉네임 및 JWT Access Token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
