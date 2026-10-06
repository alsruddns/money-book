package com.moneybook.backend.accountmanagement.service;

import com.moneybook.backend.accountmanagement.service.impl.AccountSessionServiceImpl;
import com.moneybook.backend.auth.token.JwtTokenProvider;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountSessionServiceImplTests {
    private final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final AccountSessionServiceImpl service = new AccountSessionServiceImpl(sessions, tokens);

    @Test
    void listsCurrentUsersSessionsWithoutTokenMaterial() {
        var session = session(42L, "current-key");
        when(tokens.getAccessSessionKey(org.mockito.ArgumentMatchers.any())).thenReturn("current-key");
        when(sessions.findActiveByUserUid(42L)).thenReturn(List.of(session));

        var result = service.list(authentication("42"));

        assertEquals(1, result.size());
        assertEquals(true, result.getFirst().current());
        assertEquals(9L, result.getFirst().sessionUid());
    }

    @Test
    void specificSessionLookupCannotCrossUserBoundary() {
        when(sessions.findByUidAndUserUid(9L, 42L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.revoke(authentication("42"), 9L));

        assertEquals(ErrorCode.REFRESH_SESSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void logoutAllRevokesEveryUnrevokedSession() {
        var first = session(42L, "one");
        var second = session(42L, "two");
        when(sessions.findUnrevokedByUserUid(42L)).thenReturn(List.of(first, second));

        service.revokeAll(authentication("42"));

        org.junit.jupiter.api.Assertions.assertEquals("USER_LOGOUT_ALL", first.getRevokeReason());
        org.junit.jupiter.api.Assertions.assertEquals("USER_LOGOUT_ALL", second.getRevokeReason());
        verify(sessions).findUnrevokedByUserUid(42L);
    }

    private RefreshTokenSession session(Long userUid, String key) {
        var session = RefreshTokenSession.create(userUid, key, "hash", "agent", "127.0.0.1",
                LocalDateTime.now(ZoneOffset.UTC), LocalDateTime.now(ZoneOffset.UTC).plusDays(14));
        org.springframework.test.util.ReflectionTestUtils.setField(session, "refreshSessionUid", 9L);
        return session;
    }

    private JwtAuthenticationToken authentication(String userUid) {
        Jwt jwt = Jwt.withTokenValue("access-token").header("alg", "HS256")
                .claim("sub", userUid).claim("sid", "current-key").claim("token_type", "ACCESS").build();
        return new JwtAuthenticationToken(jwt);
    }
}
