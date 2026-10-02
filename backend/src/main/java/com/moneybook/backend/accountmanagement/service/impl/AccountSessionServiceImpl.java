package com.moneybook.backend.accountmanagement.service.impl;

import com.moneybook.backend.accountmanagement.dto.RefreshSessionResponse;
import com.moneybook.backend.accountmanagement.service.AccountSessionService;
import com.moneybook.backend.auth.token.JwtTokenProvider;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountSessionServiceImpl implements AccountSessionService {
    private final RefreshSessionRepository sessions;
    private final JwtTokenProvider tokens;

    /** Lists only unexpired sessions owned by the authenticated user and marks the access token's session. */
    @Override
    @Transactional(readOnly = true)
    public List<RefreshSessionResponse> list(Authentication authentication) {
        Long userUid = userUid(authentication);
        String currentKey = tokens.getAccessSessionKey(authentication);
        return sessions.findActiveByUserUid(userUid).stream()
                .map(session -> response(session, currentKey.equals(session.getSessionKey())))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(Authentication authentication, Long sessionUid) {
        RefreshTokenSession session = sessions.findByUidAndUserUid(sessionUid, userUid(authentication))
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_SESSION_NOT_FOUND));
        session.revoke(LocalDateTime.now(ZoneOffset.UTC), "USER_REVOKED");
    }

    @Override
    @Transactional
    public void revokeCurrent(Authentication authentication) {
        RefreshTokenSession session = sessions.findBySessionKeyForUpdate(tokens.getAccessSessionKey(authentication))
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_SESSION_NOT_FOUND));
        if (!session.getUserUid().equals(userUid(authentication))) {
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }
        session.revoke(LocalDateTime.now(ZoneOffset.UTC), "USER_LOGOUT");
    }

    @Override
    @Transactional
    public void revokeAll(Authentication authentication) {
        Long userUid = userUid(authentication);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        sessions.findUnrevokedByUserUid(userUid).forEach(session -> session.revoke(now, "USER_LOGOUT_ALL"));
    }

    private Long userUid(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private RefreshSessionResponse response(RefreshTokenSession session, boolean current) {
        return new RefreshSessionResponse(session.getRefreshSessionUid(), current, session.getUserAgent(),
                session.getIpAddress(), session.getRegTime(), session.getLastUsedAt(), session.getExpiresAt());
    }
}
