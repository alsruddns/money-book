package com.moneybook.backend.auth.service;

import com.moneybook.backend.auth.dto.CurrentUserResponse;
import com.moneybook.backend.auth.dto.LoginRequest;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshRequest;
import com.moneybook.backend.auth.dto.RefreshResponse;
import com.moneybook.backend.auth.dto.SignUpReqDto;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.auth.service.impl.AuthServiceImpl;
import com.moneybook.backend.auth.token.JwtTokenProvider;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.security.ratelimit.RateLimitDecision;
import com.moneybook.backend.security.ratelimit.RateLimitProperties;
import com.moneybook.backend.security.ratelimit.RateLimiter;
import com.moneybook.backend.security.ratelimit.RateLimitExceededException;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.config.SessionSecurityProperties;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.enums.SecurityQuestionCode;
import com.moneybook.backend.recovery.RecoverySecretGenerator;
import com.moneybook.backend.recovery.repository.PasswordRecoveryRepository;
import com.moneybook.backend.recovery.repository.EmailVerificationRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceImplTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAuthRepository userAuthRepository = mock(UserAuthRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshSessionRepository refreshSessions = mock(RefreshSessionRepository.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final RateLimitProperties rateLimitProperties = new RateLimitProperties();
    private final PasswordRecoveryRepository recoveryCodes=mock(PasswordRecoveryRepository.class);
    private final EmailVerificationRepository emailVerifications=mock(EmailVerificationRepository.class);
    private final RecoverySecretGenerator secrets=new RecoverySecretGenerator();
    private final AuthServiceImpl service = createService(Clock.systemUTC());

    private AuthServiceImpl createService(Clock clock) {
        return new AuthServiceImpl(userRepository, userAuthRepository, passwordEncoder, jwtTokenProvider,
                refreshSessions, rateLimiter, rateLimitProperties, secrets, recoveryCodes,
                emailVerifications, new SessionSecurityProperties(6), clock);
    }

    private void prepareRefreshSession(RefreshTokenSession session, String presentedToken, String storedHash) {
        when(jwtTokenProvider.getRefreshTokenClaims(presentedToken)).thenReturn(
                new JwtTokenProvider.RefreshClaims(session.getUserUid(), session.getSessionKey(),
                        Instant.now().plus(Duration.ofDays(1)), Instant.now().minus(Duration.ofHours(1))));
        when(refreshSessions.findBySessionKeyForUpdate(session.getSessionKey())).thenReturn(Optional.of(session));
        when(jwtTokenProvider.hashRefreshToken(presentedToken)).thenReturn(storedHash);
        User user = mock(User.class);
        when(userRepository.findById(session.getUserUid())).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(jwtTokenProvider.createRefreshToken(session.getUserUid(), session.getSessionKey(),
                session.getExpiresAt().toInstant(ZoneOffset.UTC))).thenReturn("rotated-refresh-token");
        when(jwtTokenProvider.hashRefreshToken("rotated-refresh-token")).thenReturn("rotated-hash");
        when(jwtTokenProvider.createAccessToken(session.getUserUid(), session.getSessionKey())).thenReturn("new-access-token");
    }

    @BeforeEach
    void allowLoginRateLimitByDefault() {
        when(rateLimiter.tryAcquire(anyString(), anyString(), anyInt(), any()))
                .thenReturn(new RateLimitDecision(true, 0));
    }

    @Test
    void signUpStoresOnlyBcryptHashAndLocalIdentity() {
        SignUpReqDto request = new SignUpReqDto("new-user", "password123", "password123", "닉네임",SecurityQuestionCode.FAVORITE_FOOD,"제육볶음",null);
        User persistedUser = mock(User.class);
        when(persistedUser.getUserUid()).thenReturn(42L);
        when(persistedUser.getNickname()).thenReturn("닉네임");
        when(userAuthRepository.findByLocalLoginId("new-user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        SignUpResDto response = service.signUp(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.ACTIVE, userCaptor.getValue().getStatus());
        ArgumentCaptor<UserAuth> authCaptor = ArgumentCaptor.forClass(UserAuth.class);
        verify(userAuthRepository).save(authCaptor.capture());
        UserAuth auth = authCaptor.getValue();
        assertEquals(AuthProvider.LOCAL, auth.getProvider());
        assertEquals("new-user", auth.getLoginId());
        assertNull(auth.getProviderUserId());
        assertFalse(request.password().equals(auth.getPasswordHash()));
        assertTrue(passwordEncoder.matches(request.password(), auth.getPasswordHash()));
        assertEquals(SecurityQuestionCode.FAVORITE_FOOD.name(),auth.getSecurityQuestionCode());
        assertTrue(passwordEncoder.matches(request.securityAnswer(),auth.getSecurityAnswerHash()));
        assertEquals(42L, response.userUid());
        assertEquals("닉네임", response.nickname());
        assertEquals(8,response.recoveryCodes().size());
        verify(recoveryCodes,org.mockito.Mockito.times(8)).save(any());
    }

    @Test
    void signUpRejectsDuplicateLoginIdBeforeWriting() {
        when(userAuthRepository.findByLocalLoginId("taken")).thenReturn(Optional.of(mock(UserAuth.class)));
        SignUpReqDto request = new SignUpReqDto("taken", "password123", "password123", "닉네임",SecurityQuestionCode.FAVORITE_FOOD,"제육볶음",null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.signUp(request));

        assertEquals(ErrorCode.DUPLICATE_LOGIN_ID, exception.getErrorCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void signUpRejectsMismatchedPasswordBeforeWriting() {
        SignUpReqDto request = new SignUpReqDto("new-user", "password123", "different", "닉네임",SecurityQuestionCode.FAVORITE_FOOD,"제육볶음",null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.signUp(request));

        assertEquals(ErrorCode.PASSWORD_CONFIRM_MISMATCH, exception.getErrorCode());
        verifyNoInteractions(userRepository, userAuthRepository);
    }

    @Test
    void loginReturnsAccessTokenForActiveLocalUser() {
        UserAuth auth = mock(UserAuth.class);
        User user = mock(User.class);
        when(userAuthRepository.findByLocalLoginId("member")).thenReturn(Optional.of(auth));
        when(auth.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(auth.getUser()).thenReturn(user);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getUserUid()).thenReturn(42L);
        when(user.getNickname()).thenReturn("닉네임");
        when(jwtTokenProvider.newSessionExpiration()).thenReturn(java.time.Instant.now().plusSeconds(86400));
        when(jwtTokenProvider.createAccessToken(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.anyString())).thenReturn("signed-access-token");
        when(jwtTokenProvider.createRefreshToken(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("signed-refresh-token");
        when(jwtTokenProvider.hashRefreshToken("signed-refresh-token")).thenReturn("token-hash");

        LoginResponse response = service.login(new LoginRequest("member", "correct-password"), null, null);

        assertEquals(42L, response.userUid());
        assertEquals("닉네임", response.nickname());
        assertEquals("signed-access-token", response.accessToken());
        assertEquals("signed-refresh-token", response.refreshToken());
        ArgumentCaptor<RefreshTokenSession> created = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(refreshSessions).save(created.capture());
        assertTrue(Duration.between(Instant.now(), created.getValue().getLastUsedAt()
                .toInstant(ZoneOffset.UTC)).abs().compareTo(Duration.ofSeconds(2)) < 0);
    }

    @Test
    void loginUsesSameFailureForUnknownIdAndWrongPassword() {
        when(userAuthRepository.findByLocalLoginId("missing")).thenReturn(Optional.empty());
        UserAuth auth = mock(UserAuth.class);
        when(userAuthRepository.findByLocalLoginId("member")).thenReturn(Optional.of(auth));
        when(auth.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));

        BusinessException unknownId = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("missing", "password"), null, null));
        BusinessException wrongPassword = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("member", "wrong-password"), null, null));

        assertEquals(ErrorCode.LOGIN_FAILED, unknownId.getErrorCode());
        assertEquals(unknownId.getErrorCode(), wrongPassword.getErrorCode());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void loginRejectsInactiveUser() {
        UserAuth auth = mock(UserAuth.class);
        User user = mock(User.class);
        when(userAuthRepository.findByLocalLoginId("member")).thenReturn(Optional.of(auth));
        when(auth.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(auth.getUser()).thenReturn(user);
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("member", "correct-password"), null, null));

        assertEquals(ErrorCode.USER_INACTIVE, exception.getErrorCode());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void refreshReturnsNewAccessTokenForActiveUser() {
        User user = mock(User.class);
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        RefreshTokenSession session = RefreshTokenSession.create(42L, "session-key", "old-hash", null, null,
                now, now.plusDays(12));
        when(jwtTokenProvider.getRefreshTokenClaims("refresh-token")).thenReturn(
                new JwtTokenProvider.RefreshClaims(42L, "session-key", now.toInstant(java.time.ZoneOffset.UTC).plusSeconds(1000),
                        now.toInstant(java.time.ZoneOffset.UTC)));
        when(refreshSessions.findBySessionKeyForUpdate("session-key")).thenReturn(Optional.of(session));
        when(jwtTokenProvider.hashRefreshToken("refresh-token")).thenReturn("old-hash");
        when(jwtTokenProvider.createRefreshToken(42L, "session-key", session.getExpiresAt().toInstant(java.time.ZoneOffset.UTC)))
                .thenReturn("new-refresh-token");
        when(jwtTokenProvider.hashRefreshToken("new-refresh-token")).thenReturn("new-hash");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(jwtTokenProvider.createAccessToken(42L, "session-key")).thenReturn("new-access-token");

        RefreshResponse response = service.refresh(new RefreshRequest("refresh-token"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals("new-hash", session.getRefreshTokenHash());
    }

    @Test
    void refreshWithinIdleTimeoutRotatesTokenAndUpdatesLastUsedAtUsingUtcInstant() {
        Instant now = Instant.parse("2026-10-08T00:00:00Z");
        LocalDateTime lastUsed = LocalDateTime.ofInstant(now.minus(Duration.ofHours(6)).plus(Duration.ofMinutes(1)), ZoneOffset.UTC);
        LocalDateTime absoluteExpiry = LocalDateTime.ofInstant(now.plus(Duration.ofDays(14)), ZoneOffset.UTC);
        RefreshTokenSession session = RefreshTokenSession.create(42L, "session-key", "old-hash", null, null,
                lastUsed, absoluteExpiry);
        prepareRefreshSession(session, "refresh-token", "old-hash");
        AuthServiceImpl fixedClockService = createService(Clock.fixed(now, ZoneOffset.ofHours(9)));

        RefreshResponse response = fixedClockService.refresh(new RefreshRequest("refresh-token"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals("rotated-refresh-token", response.refreshToken());
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), session.getLastUsedAt());
        assertEquals(absoluteExpiry, session.getExpiresAt(), "rotation must preserve the 14-day absolute expiry");
    }

    @Test
    void refreshAtSixHourIdleBoundaryRevokesSessionAndReturnsGenericInvalidRefresh() {
        Instant now = Instant.parse("2026-10-08T00:00:00Z");
        RefreshTokenSession session = RefreshTokenSession.create(42L, "session-key", "stored-hash", null, null,
                LocalDateTime.ofInstant(now.minus(Duration.ofHours(6)), ZoneOffset.UTC),
                LocalDateTime.ofInstant(now.plus(Duration.ofDays(14)), ZoneOffset.UTC));
        prepareRefreshSession(session, "refresh-token", "stored-hash");
        AuthServiceImpl fixedClockService = createService(Clock.fixed(now, ZoneOffset.ofHours(-7)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fixedClockService.refresh(new RefreshRequest("refresh-token")));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("IDLE_TIMEOUT", session.getRevokeReason());
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), session.getRevokedAt());
        verify(jwtTokenProvider, never()).createRefreshToken(any(), anyString(), any());
        verify(jwtTokenProvider, never()).createAccessToken(any(), anyString());
    }

    @Test
    void refreshAfterSixHourIdleTimeoutRevokesSession() {
        Instant now = Instant.parse("2026-10-08T00:00:00Z");
        RefreshTokenSession session = RefreshTokenSession.create(42L, "session-key", "stored-hash", null, null,
                LocalDateTime.ofInstant(now.minus(Duration.ofHours(6)).minusNanos(1), ZoneOffset.UTC),
                LocalDateTime.ofInstant(now.plus(Duration.ofDays(14)), ZoneOffset.UTC));
        prepareRefreshSession(session, "refresh-token", "stored-hash");
        AuthServiceImpl fixedClockService = createService(Clock.fixed(now, ZoneOffset.UTC));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fixedClockService.refresh(new RefreshRequest("refresh-token")));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("IDLE_TIMEOUT", session.getRevokeReason());
    }

    @Test
    void absoluteExpiryAndRevokedSessionsRemainInvalidRegardlessOfIdleUse() {
        Instant now = Instant.parse("2026-10-08T00:00:00Z");
        RefreshTokenSession expired = RefreshTokenSession.create(42L, "expired", "hash", null, null,
                LocalDateTime.ofInstant(now, ZoneOffset.UTC), LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        RefreshTokenSession revoked = RefreshTokenSession.create(42L, "revoked", "hash", null, null,
                LocalDateTime.ofInstant(now, ZoneOffset.UTC), LocalDateTime.ofInstant(now.plus(Duration.ofDays(14)), ZoneOffset.UTC));
        revoked.revoke(LocalDateTime.ofInstant(now.minusSeconds(1), ZoneOffset.UTC), "USER_LOGOUT");
        AuthServiceImpl fixedClockService = createService(Clock.fixed(now, ZoneOffset.UTC));
        when(jwtTokenProvider.getRefreshTokenClaims("expired-token")).thenReturn(
                new JwtTokenProvider.RefreshClaims(42L, "expired", now.plusSeconds(60), now.minusSeconds(60)));
        when(jwtTokenProvider.getRefreshTokenClaims("revoked-token")).thenReturn(
                new JwtTokenProvider.RefreshClaims(42L, "revoked", now.plusSeconds(60), now.minusSeconds(60)));
        when(refreshSessions.findBySessionKeyForUpdate("expired")).thenReturn(Optional.of(expired));
        when(refreshSessions.findBySessionKeyForUpdate("revoked")).thenReturn(Optional.of(revoked));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, assertThrows(BusinessException.class,
                () -> fixedClockService.refresh(new RefreshRequest("expired-token"))).getErrorCode());
        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, assertThrows(BusinessException.class,
                () -> fixedClockService.refresh(new RefreshRequest("revoked-token"))).getErrorCode());
        assertEquals("USER_LOGOUT", revoked.getRevokeReason());
        assertEquals(Duration.ofHours(6), new SessionSecurityProperties(6).idleTimeout());
    }

    @Test
    void refreshRejectsUnknownSession() {
        when(jwtTokenProvider.getRefreshTokenClaims("refresh-token")).thenReturn(
                new JwtTokenProvider.RefreshClaims(42L, "session-key", java.time.Instant.now().plusSeconds(1000),
                        java.time.Instant.now()));
        when(refreshSessions.findBySessionKeyForUpdate("session-key")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshRequest("refresh-token")));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        verify(jwtTokenProvider).getRefreshTokenClaims("refresh-token");
    }

    @Test
    void refreshRejectsInactiveUser() {
        User user = mock(User.class);
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        RefreshTokenSession session = RefreshTokenSession.create(42L, "session-key", "stored-hash", null, null,
                now, now.plusDays(1));
        when(jwtTokenProvider.getRefreshTokenClaims("refresh-token")).thenReturn(
                new JwtTokenProvider.RefreshClaims(42L, "session-key", now.toInstant(java.time.ZoneOffset.UTC).plusSeconds(1000),
                        now.toInstant(java.time.ZoneOffset.UTC)));
        when(refreshSessions.findBySessionKeyForUpdate("session-key")).thenReturn(Optional.of(session));
        when(jwtTokenProvider.hashRefreshToken("refresh-token")).thenReturn("stored-hash");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshRequest("refresh-token")));

        assertEquals(ErrorCode.USER_INACTIVE, exception.getErrorCode());
    }

    @Test
    void signUpConsumesGrantAndStoresNormalizedVerifiedEmail() {
        String token = "signup-email-grant";
        var verification = new com.moneybook.backend.entity.EmailVerification(null, "  Alice@Example.com  ",
                "SIGNUP", secrets.sha256("123456"), java.time.LocalDateTime.now().plusMinutes(10),
                java.time.LocalDateTime.now().plusSeconds(60));
        verification.issueGrant(secrets.sha256(token), java.time.LocalDateTime.now().plusMinutes(10));
        when(emailVerifications.findActiveGrant(secrets.sha256(token), "SIGNUP")).thenReturn(Optional.of(verification));
        when(userAuthRepository.existsVerifiedEmail("alice@example.com")).thenReturn(false);
        when(userAuthRepository.findByLocalLoginId("new-email-user")).thenReturn(Optional.empty());
        User persistedUser = mock(User.class);
        when(persistedUser.getUserUid()).thenReturn(43L);
        when(persistedUser.getNickname()).thenReturn("Email User");
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        SignUpReqDto request = new SignUpReqDto("new-email-user", "password123", "password123", "Email User",
                SecurityQuestionCode.FAVORITE_COLOR, "Blue", token);
        service.signUp(request);

        ArgumentCaptor<UserAuth> authCaptor = ArgumentCaptor.forClass(UserAuth.class);
        verify(userAuthRepository).save(authCaptor.capture());
        assertEquals("alice@example.com", authCaptor.getValue().getVerifiedEmail());
        assertTrue(verification.getGrantConsumedAt() != null);
        assertEquals(ErrorCode.VALIDATION_FAILED,
                assertThrows(BusinessException.class, () -> service.signUp(request)).getErrorCode());
    }

    @Test
    void signUpRejectsDuplicateNormalizedEmailBeforeCreatingUserOrConsumingGrant() {
        String token = "duplicate-signup-email-grant";
        var verification = new com.moneybook.backend.entity.EmailVerification(null, " ALICE@example.COM ",
                "SIGNUP", secrets.sha256("123456"), java.time.LocalDateTime.now().plusMinutes(10),
                java.time.LocalDateTime.now().plusSeconds(60));
        verification.issueGrant(secrets.sha256(token), java.time.LocalDateTime.now().plusMinutes(10));
        when(emailVerifications.findActiveGrant(secrets.sha256(token), "SIGNUP")).thenReturn(Optional.of(verification));
        when(userAuthRepository.existsVerifiedEmail("alice@example.com")).thenReturn(true);
        when(userAuthRepository.findByLocalLoginId("duplicate-email-user")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.signUp(
                new SignUpReqDto("duplicate-email-user", "password123", "password123", "Email User",
                        SecurityQuestionCode.FAVORITE_COLOR, "Blue", token)));

        assertEquals(ErrorCode.EMAIL_ALREADY_IN_USE, exception.getErrorCode());
        verifyNoInteractions(userRepository);
        verify(userAuthRepository, never()).save(any());
        verify(emailVerifications, never()).save(any());
        assertNull(verification.getGrantConsumedAt());
    }

    @Test
    void signupMapsUniqueIndexRaceToEmailAlreadyInUse() {
        String token = "racing-signup-email-grant";
        var verification = new com.moneybook.backend.entity.EmailVerification(null, "race@example.com", "SIGNUP",
                secrets.sha256("123456"), java.time.LocalDateTime.now().plusMinutes(10),
                java.time.LocalDateTime.now().plusSeconds(60));
        verification.issueGrant(secrets.sha256(token), java.time.LocalDateTime.now().plusMinutes(10));
        when(emailVerifications.findActiveGrant(secrets.sha256(token), "SIGNUP")).thenReturn(Optional.of(verification));
        when(userAuthRepository.existsVerifiedEmail("race@example.com")).thenReturn(false);
        when(userAuthRepository.findByLocalLoginId("racing-user")).thenReturn(Optional.empty());
        User persistedUser = mock(User.class);
        when(persistedUser.getUserUid()).thenReturn(44L);
        when(persistedUser.getNickname()).thenReturn("Race User");
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);
        var violation = new org.hibernate.exception.ConstraintViolationException("duplicate", null,
                "insert into user_auth", "uq_user_auth_verified_email");
        when(userAuthRepository.save(any(UserAuth.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("unique email", violation));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.signUp(
                new SignUpReqDto("racing-user", "password123", "password123", "Race User",
                        SecurityQuestionCode.FAVORITE_COLOR, "Blue", token)));

        assertEquals(ErrorCode.EMAIL_ALREADY_IN_USE, exception.getErrorCode());
    }

    @Test
    void loginRateLimitUsesNormalizedHashedLoginIdAndStopsBeforeCredentialLookup() {
        when(rateLimiter.tryAcquire(org.mockito.ArgumentMatchers.eq("login-id-5m"), anyString(), anyInt(), any()))
                .thenReturn(new RateLimitDecision(false, 37));

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> service.login(new LoginRequest(" Member ", "wrong-password"), null, null));

        assertEquals(37, exception.getRetryAfterSeconds());
        verifyNoInteractions(userAuthRepository);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryAcquire(org.mockito.ArgumentMatchers.eq("login-id-5m"), key.capture(),
                org.mockito.ArgumentMatchers.eq(rateLimitProperties.getLoginIdPer5Minutes()),
                org.mockito.ArgumentMatchers.eq(java.time.Duration.ofMinutes(5)));
        assertEquals(64, key.getValue().length());
        assertFalse("member".equals(key.getValue()));
    }

    @Test
    void refreshTokenReuseRevokesSessionAndFails() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        RefreshTokenSession session = RefreshTokenSession.create(42L, "session-key", "current-hash", null, null,
                now, now.plusDays(1));
        when(jwtTokenProvider.getRefreshTokenClaims("old-token")).thenReturn(
                new JwtTokenProvider.RefreshClaims(42L, "session-key", now.toInstant(java.time.ZoneOffset.UTC).plusSeconds(1000),
                        now.toInstant(java.time.ZoneOffset.UTC)));
        when(refreshSessions.findBySessionKeyForUpdate("session-key")).thenReturn(Optional.of(session));
        when(jwtTokenProvider.hashRefreshToken("old-token")).thenReturn("previous-hash");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.refresh(new RefreshRequest("old-token")));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertEquals("TOKEN_REUSE", session.getRevokeReason());
    }

    @Test
    void currentUserReturnsActiveUserProfile() {
        User user = mock(User.class);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(user.getUserUid()).thenReturn(42L);
        when(user.getNickname()).thenReturn("닉네임");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        CurrentUserResponse response = service.currentUser(authentication("42"));

        assertEquals(42L, response.userUid());
        assertEquals("닉네임", response.nickname());
        assertEquals(UserStatus.ACTIVE, response.status());
    }

    @Test
    void currentUserRejectsMissingUser() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.currentUser(authentication("42")));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void currentUserRejectsInactiveUser() {
        User user = mock(User.class);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.currentUser(authentication("42")));

        assertEquals(ErrorCode.USER_INACTIVE, exception.getErrorCode());
    }

    private JwtAuthenticationToken authentication(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", subject)
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
