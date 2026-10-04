package com.moneybook.backend.auth.service.impl;

import com.moneybook.backend.auth.dto.CurrentUserResponse;
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
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.security.ratelimit.RateLimitExceededException;
import com.moneybook.backend.security.ratelimit.RateLimitProperties;
import com.moneybook.backend.security.ratelimit.RateLimiter;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.time.Duration;
import com.moneybook.backend.entity.PasswordRecoveryCode;
import com.moneybook.backend.recovery.RecoverySecretGenerator;
import com.moneybook.backend.recovery.EmailAddressNormalizer;
import com.moneybook.backend.recovery.VerifiedEmailConstraint;
import com.moneybook.backend.recovery.repository.PasswordRecoveryRepository;
import com.moneybook.backend.recovery.repository.EmailVerificationRepository;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOCAL_LOGIN_ID_CONSTRAINT = "uq_user_auth_local_login_id";
    private static final String DUMMY_PASSWORD_HASH = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshSessionRepository refreshSessions;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final RecoverySecretGenerator recoverySecrets;
    private final PasswordRecoveryRepository recoveryCodes;
    private final EmailVerificationRepository emailVerifications;

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
        if (request.securityQuestionCode() == null || request.securityAnswer() == null
                || request.securityAnswer().isBlank()
                || request.securityAnswer().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!request.securityAnswer().equals(request.securityAnswer().strip())) {
            throw new BusinessException(ErrorCode.SECURITY_ANSWER_WHITESPACE);
        }
        if (userAuthRepository.findByLocalLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        com.moneybook.backend.entity.EmailVerification signupVerification = null;
        String verifiedEmail = null;
        if (request.emailVerificationToken() != null && !request.emailVerificationToken().isBlank()) {
            signupVerification = emailVerifications.findActiveGrant(
                    recoverySecrets.sha256(request.emailVerificationToken()), "SIGNUP")
                    .filter(v -> v.getUserUid() == null && v.grantActive(LocalDateTime.now()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));
            verifiedEmail = EmailAddressNormalizer.normalize(signupVerification.getEmail());
            if (userAuthRepository.existsVerifiedEmail(verifiedEmail)) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_IN_USE);
            }
        }

        User user = userRepository.save(User.create(request.nickname(), null));
        UserAuth userAuth = UserAuth.local(user, request.loginId(), passwordEncoder.encode(request.password()));
        userAuth.changeSecurityQuestion(request.securityQuestionCode().name(), passwordEncoder.encode(request.securityAnswer()));
        if (signupVerification != null) {
            signupVerification.consumeGrant(LocalDateTime.now());
            userAuth.changeVerifiedEmail(verifiedEmail, LocalDateTime.now());
            emailVerifications.save(signupVerification);
        }
        try {
            userAuthRepository.save(userAuth);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLocalLoginId(exception)) {
                throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
            }
            if (VerifiedEmailConstraint.wasViolated(exception)) throw new BusinessException(ErrorCode.EMAIL_ALREADY_IN_USE);
            throw exception;
        }
        var codes = recoverySecrets.newRecoveryCodes(8);
        codes.forEach(code -> recoveryCodes.save(new PasswordRecoveryCode(user.getUserUid(), recoverySecrets.sha256(code))));
        return new SignUpResDto(user.getUserUid(), user.getNickname(), codes);
    }

    /** Validates LOCAL credentials and active user state before issuing an Access Token. */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String normalizedLoginId = request.loginId().strip().toLowerCase(Locale.ROOT);
        var loginLimit = rateLimiter.tryAcquire("login-id-5m", hashLoginId(normalizedLoginId),
                rateLimitProperties.getLoginIdPer5Minutes(), Duration.ofMinutes(5));
        if (!loginLimit.allowed()) throw new RateLimitExceededException(loginLimit.retryAfterSeconds());
        UserAuth userAuth = userAuthRepository.findByLocalLoginId(request.loginId()).orElse(null);
        if (userAuth == null) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        User user = userAuth.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        String sessionKey = UUID.randomUUID().toString();
        var expiresAt = jwtTokenProvider.newSessionExpiration();
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserUid(), sessionKey, expiresAt);
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserUid(), sessionKey);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        refreshSessions.save(RefreshTokenSession.create(user.getUserUid(), sessionKey,
                jwtTokenProvider.hashRefreshToken(refreshToken), userAgent, ipAddress,
                now, LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)));
        return new LoginResponse(user.getUserUid(), user.getNickname(), accessToken, refreshToken,
                userAuth.isPasswordChangeRequired());
    }

    /** Validates and rotates the persisted Refresh Session before returning a new token pair. */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshResponse refresh(RefreshRequest request) {
        JwtTokenProvider.RefreshClaims claims = jwtTokenProvider.getRefreshTokenClaims(request.refreshToken());
        RefreshTokenSession session = refreshSessions.findBySessionKeyForUpdate(claims.sessionKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!session.getUserUid().equals(claims.userUid()) || !session.isActiveAt(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String presentedHash = jwtTokenProvider.hashRefreshToken(request.refreshToken());
        if (!MessageDigest.isEqual(session.getRefreshTokenHash().getBytes(StandardCharsets.US_ASCII),
                presentedHash.getBytes(StandardCharsets.US_ASCII))) {
            session.revoke(now, "TOKEN_REUSE");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userUid = claims.userUid();
        User user = userRepository.findById(userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        String refreshToken = jwtTokenProvider.createRefreshToken(userUid, claims.sessionKey(),
                session.getExpiresAt().toInstant(ZoneOffset.UTC));
        String accessToken = jwtTokenProvider.createAccessToken(userUid, claims.sessionKey());
        session.rotate(jwtTokenProvider.hashRefreshToken(refreshToken), now);
        return new RefreshResponse(accessToken, refreshToken);
    }

    /** Revokes only the session identified by the authenticated Access Token. */
    @Override
    @Transactional
    public void logout(Authentication authentication) {
        String sessionKey = jwtTokenProvider.getAccessSessionKey(authentication);
        Long userUid;
        try {
            userUid = Long.valueOf(authentication.getName());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        RefreshTokenSession session = refreshSessions.findBySessionKeyForUpdate(sessionKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_SESSION_NOT_FOUND));
        if (!session.getUserUid().equals(userUid)) {
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }
        session.revoke(LocalDateTime.now(ZoneOffset.UTC), "USER_LOGOUT");
    }

    /** Reads the verified JWT principal and returns the active user's current persisted profile. */
    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        Long userUid;
        try {
            userUid = Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        User user = userRepository.findById(userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        boolean forced = userAuthRepository.findLocalByUserUid(userUid)
                .map(UserAuth::isPasswordChangeRequired).orElse(false);
        return new CurrentUserResponse(user.getUserUid(), user.getNickname(), user.getStatus(), user.getSystemRole(), forced);
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

    private String hashLoginId(String loginId) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(loginId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

}
