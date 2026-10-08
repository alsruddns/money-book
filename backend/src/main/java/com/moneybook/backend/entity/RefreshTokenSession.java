package com.moneybook.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/** 서버가 상태와 회전을 관리하는 Refresh Token 세션이다. 원문 토큰은 저장하지 않는다. */
@Getter
@Entity
@Table(name = "refresh_token_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_session_uid")
    private Long refreshSessionUid;

    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    @Column(name = "session_key", nullable = false, unique = true, length = 36)
    private String sessionKey;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 30)
    private String revokeReason;

    private RefreshTokenSession(Long userUid, String sessionKey, String refreshTokenHash,
                                String userAgent, String ipAddress,
                                LocalDateTime now, LocalDateTime expiresAt) {
        this.userUid = Objects.requireNonNull(userUid, "userUid");
        this.sessionKey = Objects.requireNonNull(sessionKey, "sessionKey");
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "refreshTokenHash");
        this.userAgent = limit(userAgent, 500);
        this.ipAddress = limit(ipAddress, 100);
        this.lastUsedAt = Objects.requireNonNull(now, "now");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public static RefreshTokenSession create(Long userUid, String sessionKey, String refreshTokenHash,
                                             String userAgent, String ipAddress,
                                             LocalDateTime now, LocalDateTime expiresAt) {
        return new RefreshTokenSession(userUid, sessionKey, refreshTokenHash,
                userAgent, ipAddress, now, expiresAt);
    }

    /** Rotates only the token hash while preserving the original absolute expiry. */
    public void rotate(String refreshTokenHash, LocalDateTime now) {
        if (revokedAt != null) {
            throw new IllegalStateException("Revoked sessions cannot be rotated");
        }
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "refreshTokenHash");
        this.lastUsedAt = Objects.requireNonNull(now, "now");
    }

    public void revoke(LocalDateTime now, String reason) {
        if (revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(now, "now");
            this.revokeReason = Objects.requireNonNull(reason, "reason");
        }
    }

    public boolean isActiveAt(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /** Treats the exact idle-timeout boundary as expired. */
    public boolean isIdleAt(LocalDateTime now, Duration idleTimeout) {
        return !lastUsedAt.plus(idleTimeout).isAfter(now);
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
