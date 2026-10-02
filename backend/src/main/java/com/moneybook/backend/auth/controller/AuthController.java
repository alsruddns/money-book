package com.moneybook.backend.auth.controller;

import com.moneybook.backend.auth.dto.CurrentUserResponse;
import com.moneybook.backend.auth.dto.LoginRequest;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshRequest;
import com.moneybook.backend.auth.dto.RefreshResponse;
import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.servlet.http.HttpServletRequest;

/**
 * LOCAL 회원가입, 로그인, Access Token 재발급 및 현재 사용자 조회 API를 제공한다.
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
     * 인증 없이 LOCAL 계정의 로그인 ID와 비밀번호를 검증하고 두 종류의 토큰을 발급한다.
     *
     * @param request 로그인 ID와 비밀번호
     * @return 사용자 UID, 닉네임, JWT Access Token 및 Refresh Token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()));
    }

    /**
     * 인증 없이 받은 Refresh Token을 검증하고 활성 사용자의 새 Access Token을 발급한다.
     * 서명·만료·세션 상태를 검증하고 refresh token을 회전한다.
     *
     * @param request 기존 Refresh Token
     * @return 새 JWT Access Token과 Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /** Access Token에 연결된 현재 Refresh Session을 폐기한다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout(authentication);
        return ResponseEntity.noContent().build();
    }

    /**
     * Access Token으로 인증된 사용자의 기본 정보를 조회한다.
     * 사용자 존재 여부와 활성 상태는 서비스에서 확인한다.
     *
     * @param authentication Spring Security에서 검증한 인증 정보
     * @return 사용자 UID, 닉네임 및 상태
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication));
    }
}
