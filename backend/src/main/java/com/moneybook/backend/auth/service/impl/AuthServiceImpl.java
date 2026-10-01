package com.moneybook.backend.auth.service.impl;

import com.moneybook.backend.auth.dto.LoginRequest;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshRequest;
import com.moneybook.backend.auth.dto.RefreshResponse;
import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.auth.token.JwtTokenProvider;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOCAL_LOGIN_ID_CONSTRAINT = "uq_user_auth_local_login_id";

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** Creates the user and LOCAL credentials atomically; a duplicate login ID rolls both inserts back. */
    @Override
    @Transactional
    public SignUpResDto signUp(SignUpReqDto request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_LENGTH);
        }
        if (userAuthRepository.findByLocalLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        User user = userRepository.save(User.create(request.nickname(), null));
        UserAuth userAuth = UserAuth.local(user, request.loginId(), passwordEncoder.encode(request.password()));
        try {
            userAuthRepository.save(userAuth);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLocalLoginId(exception)) {
                throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
            }
            throw exception;
        }
        return new SignUpResDto(user.getUserUid(), user.getNickname());
    }

    /** Validates LOCAL credentials and active user state before issuing an Access Token. */
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserAuth userAuth = userAuthRepository.findByLocalLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        User user = userAuth.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        return new LoginResponse(user.getUserUid(), user.getNickname(),
                jwtTokenProvider.createAccessToken(user.getUserUid()),
                jwtTokenProvider.createRefreshToken(user.getUserUid()));
    }

    /** Reissues only an Access Token after validating a Refresh Token and its active user. */
    @Override
    @Transactional(readOnly = true)
    public RefreshResponse refresh(RefreshRequest request) {
        Long userUid = jwtTokenProvider.getRefreshTokenUserUid(request.refreshToken());
        User user = userRepository.findById(userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        return new RefreshResponse(jwtTokenProvider.createAccessToken(userUid));
    }

    private boolean isDuplicateLocalLoginId(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && LOCAL_LOGIN_ID_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
