package com.moneybook.backend.auth.token;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.enums.JwtTokenType;
import org.springframework.beans.factory.annotation.Value;
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

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshDecoder;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(JwtEncoder jwtEncoder, SecretKey jwtSecretKey,
                            @Value("${jwt.access-token-expiration}") Duration accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") Duration refreshTokenExpiration) {
        requirePositive(accessTokenExpiration, "Access");
        requirePositive(refreshTokenExpiration, "Refresh");
        this.jwtEncoder = jwtEncoder;
        this.refreshDecoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** Signs a short lived Access Token with the user's stable UID as its subject. */
    public String createAccessToken(Long userUid) {
        return createToken(userUid, JwtTokenType.ACCESS, accessTokenExpiration);
    }

    /** Signs a Refresh Token that cannot be used as a Bearer token for general APIs. */
    public String createRefreshToken(Long userUid) {
        return createToken(userUid, JwtTokenType.REFRESH, refreshTokenExpiration);
    }

    /** Verifies the signature, expiry and REFRESH type before returning the user UID. */
    public Long getRefreshTokenUserUid(String refreshToken) {
        try {
            Jwt jwt = refreshDecoder.decode(refreshToken);
            if (!JwtTokenType.REFRESH.name().equals(jwt.getClaimAsString(JwtTokenType.CLAIM_NAME))) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            return Long.valueOf(jwt.getSubject());
        } catch (JwtException | NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private String createToken(Long userUid, JwtTokenType type, Duration expiration) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userUid.toString())
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim(JwtTokenType.CLAIM_NAME, type.name())
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
