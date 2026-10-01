package com.moneybook.backend.auth.service.impl;

import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
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
