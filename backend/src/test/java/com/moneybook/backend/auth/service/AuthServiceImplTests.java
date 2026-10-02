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
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
    private final AuthServiceImpl service = new AuthServiceImpl(
            userRepository, userAuthRepository, passwordEncoder, jwtTokenProvider, refreshSessions,
            rateLimiter, rateLimitProperties);

    @BeforeEach
    void allowLoginRateLimitByDefault() {
        when(rateLimiter.tryAcquire(anyString(), anyString(), anyInt(), any()))
                .thenReturn(new RateLimitDecision(true, 0));
    }

    @Test
    void signUpStoresOnlyBcryptHashAndLocalIdentity() {
        SignUpReqDto request = new SignUpReqDto("new-user", "password123", "password123", "닉네임");
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
        assertEquals(42L, response.userUid());
        assertEquals("닉네임", response.nickname());
    }

    @Test
    void signUpRejectsDuplicateLoginIdBeforeWriting() {
        when(userAuthRepository.findByLocalLoginId("taken")).thenReturn(Optional.of(mock(UserAuth.class)));
        SignUpReqDto request = new SignUpReqDto("taken", "password123", "password123", "닉네임");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.signUp(request));

        assertEquals(ErrorCode.DUPLICATE_LOGIN_ID, exception.getErrorCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void signUpRejectsMismatchedPasswordBeforeWriting() {
        SignUpReqDto request = new SignUpReqDto("new-user", "password123", "different", "닉네임");

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
        verify(refreshSessions).save(any());
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
