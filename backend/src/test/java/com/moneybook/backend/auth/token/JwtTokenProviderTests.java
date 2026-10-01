package com.moneybook.backend.auth.token;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.enums.JwtTokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTests {

    private final JwtConfig config = new JwtConfig();
    private SecretKey secretKey;
    private JwtEncoder encoder;
    private JwtDecoder accessDecoder;
    private JwtDecoder rawDecoder;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        secretKey = config.jwtSecretKey(Base64.getEncoder().encodeToString(keyBytes));
        encoder = config.jwtEncoder(secretKey);
        accessDecoder = config.jwtDecoder(secretKey);
        rawDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        provider = new JwtTokenProvider(encoder, secretKey, Duration.ofHours(1), Duration.ofDays(14));
    }

    @Test
    void accessAndRefreshTokensHaveDifferentTypesAndExpirations() {
        Jwt access = accessDecoder.decode(provider.createAccessToken(42L));
        String refreshToken = provider.createRefreshToken(42L);
        Jwt refresh = rawDecoder.decode(refreshToken);

        assertEquals("42", access.getSubject());
        assertEquals("42", refresh.getSubject());
        assertEquals("HS256", access.getHeaders().get("alg"));
        assertEquals(JwtTokenType.ACCESS.name(), access.getClaimAsString(JwtTokenType.CLAIM_NAME));
        assertEquals(JwtTokenType.REFRESH.name(), refresh.getClaimAsString(JwtTokenType.CLAIM_NAME));
        assertEquals(Duration.ofHours(1), Duration.between(access.getIssuedAt(), access.getExpiresAt()));
        assertEquals(Duration.ofDays(14), Duration.between(refresh.getIssuedAt(), refresh.getExpiresAt()));
        assertEquals(42L, provider.getRefreshTokenUserUid(refreshToken));
        assertEquals(42L, provider.getRefreshTokenUserUid(refreshToken));
    }

    @Test
    void accessTokenCannotRefreshAndRefreshTokenCannotAuthenticateAsBearer() {
        String accessToken = provider.createAccessToken(42L);
        String refreshToken = provider.createRefreshToken(42L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> provider.getRefreshTokenUserUid(accessToken));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        assertThrows(JwtException.class, () -> accessDecoder.decode(refreshToken));
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(past.minus(Duration.ofDays(14)))
                .expiresAt(past)
                .claim(JwtTokenType.CLAIM_NAME, JwtTokenType.REFRESH.name())
                .build();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> provider.getRefreshTokenUserUid(expired));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    @Test
    void malformedAndWronglySignedRefreshTokensAreRejected() {
        byte[] otherKeyBytes = new byte[32];
        new SecureRandom().nextBytes(otherKeyBytes);
        SecretKey otherKey = config.jwtSecretKey(Base64.getEncoder().encodeToString(otherKeyBytes));
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                config.jwtEncoder(otherKey), otherKey, Duration.ofHours(1), Duration.ofDays(14));

        BusinessException malformed = assertThrows(BusinessException.class,
                () -> provider.getRefreshTokenUserUid("not-a-jwt"));
        BusinessException wrongSignature = assertThrows(BusinessException.class,
                () -> provider.getRefreshTokenUserUid(otherProvider.createRefreshToken(42L)));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, malformed.getErrorCode());
        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, wrongSignature.getErrorCode());
    }
}
