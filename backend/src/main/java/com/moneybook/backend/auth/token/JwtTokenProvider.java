package com.moneybook.backend.auth.token;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.enums.JwtTokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class JwtTokenProvider {
    public static final String SESSION_ID_CLAIM = "sid";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshDecoder;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;
    private final Clock clock;

    public JwtTokenProvider(JwtEncoder jwtEncoder, SecretKey jwtSecretKey,
                            Duration accessTokenExpiration, Duration refreshTokenExpiration) {
        this(jwtEncoder, jwtSecretKey, accessTokenExpiration, refreshTokenExpiration, Clock.systemUTC());
    }

    @Autowired
    public JwtTokenProvider(JwtEncoder jwtEncoder, SecretKey jwtSecretKey,
                            @Value("${jwt.access-token-expiration}") Duration accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") Duration refreshTokenExpiration,
                            Clock sessionClock) {
        requirePositive(accessTokenExpiration, "Access");
        requirePositive(refreshTokenExpiration, "Refresh");
        this.jwtEncoder = jwtEncoder;
        this.refreshDecoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.clock = sessionClock;
    }

    /** Signs a short lived Access Token with the user's stable UID as its subject. */
    public String createAccessToken(Long userUid) {
        return createToken(userUid, JwtTokenType.ACCESS, accessTokenExpiration);
    }

    /** Signs an Access Token associated with the current refresh session. */
    public String createAccessToken(Long userUid, String sessionKey) {
        return createToken(userUid, JwtTokenType.ACCESS, sessionKey, clock.instant().plus(accessTokenExpiration));
    }

    /** Signs a Refresh Token that cannot be used as a Bearer token for general APIs. */
    public String createRefreshToken(Long userUid) {
        return createToken(userUid, JwtTokenType.REFRESH, refreshTokenExpiration);
    }

    /** Signs a session-bound Refresh Token with its fixed absolute expiration. */
    public String createRefreshToken(Long userUid, String sessionKey, Instant expiresAt) {
        return createToken(userUid, JwtTokenType.REFRESH, sessionKey, expiresAt);
    }

    /** Verifies the signature, expiry and REFRESH type before returning the user UID. */
    public Long getRefreshTokenUserUid(String refreshToken) {
        return getRefreshTokenClaims(refreshToken).userUid();
    }

    /** Verifies a refresh JWT and extracts only the claims needed to find its server-side session. */
    public RefreshClaims getRefreshTokenClaims(String refreshToken) {
        try {
            Jwt jwt = refreshDecoder.decode(refreshToken);
            String sessionKey = jwt.getClaimAsString(SESSION_ID_CLAIM);
            if (!JwtTokenType.REFRESH.name().equals(jwt.getClaimAsString(JwtTokenType.CLAIM_NAME))
                    || sessionKey == null || sessionKey.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            return new RefreshClaims(Long.valueOf(jwt.getSubject()), sessionKey,
                    jwt.getExpiresAt(), jwt.getIssuedAt());
        } catch (JwtException | NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /** Extracts the authenticated access token's session key for account session operations. */
    public String getAccessSessionKey(org.springframework.security.core.Authentication authentication) {
        if (!(authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token)
                || !JwtTokenType.ACCESS.name().equals(token.getToken().getClaimAsString(JwtTokenType.CLAIM_NAME))) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        String sessionKey = token.getToken().getClaimAsString(SESSION_ID_CLAIM);
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        return sessionKey;
    }

    /** Produces a fixed-size one-way digest; refresh token material is never persisted. */
    public String hashRefreshToken(String refreshToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public Instant newSessionExpiration() {
        return clock.instant().plus(refreshTokenExpiration).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }

    public record RefreshClaims(Long userUid, String sessionKey, Instant expiresAt, Instant issuedAt) { }

    private String createToken(Long userUid, JwtTokenType type, Duration expiration) {
        return createToken(userUid, type, UUID.randomUUID().toString(), clock.instant().plus(expiration));
    }

    private String createToken(Long userUid, JwtTokenType type, String sessionKey, Instant expiresAt) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userUid.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtTokenType.CLAIM_NAME, type.name())
                .claim(SESSION_ID_CLAIM, sessionKey)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static void requirePositive(Duration expiration, String tokenName) {
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException(tokenName + " token expiration must be positive");
        }
    }
}
