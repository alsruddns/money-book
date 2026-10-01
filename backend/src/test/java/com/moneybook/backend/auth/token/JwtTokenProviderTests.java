package com.moneybook.backend.auth.token;

import com.moneybook.backend.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTests {

    @Test
    void accessTokenIsSignedAndContainsUserUidAndExpiration() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        JwtConfig config = new JwtConfig();
        SecretKey secretKey = config.jwtSecretKey(Base64.getEncoder().encodeToString(keyBytes));
        JwtEncoder encoder = config.jwtEncoder(secretKey);
        JwtDecoder decoder = config.jwtDecoder(secretKey);
        JwtTokenProvider provider = new JwtTokenProvider(encoder, Duration.ofHours(1));

        Jwt token = decoder.decode(provider.createAccessToken(42L));

        assertEquals("42", token.getSubject());
        assertEquals("HS256", token.getHeaders().get("alg"));
        assertTrue(token.getIssuedAt().isBefore(Instant.now().plusSeconds(1)));
        assertEquals(Duration.ofHours(1), Duration.between(token.getIssuedAt(), token.getExpiresAt()));
    }
}
