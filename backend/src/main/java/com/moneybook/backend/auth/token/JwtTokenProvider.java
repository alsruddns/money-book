package com.moneybook.backend.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenExpiration;

    public JwtTokenProvider(JwtEncoder jwtEncoder,
                            @Value("${jwt.access-token-expiration}") Duration accessTokenExpiration) {
        if (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative()) {
            throw new IllegalArgumentException("JWT access token expiration must be positive");
        }
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /** Signs a short lived Access Token with the user's stable UID as its subject. */
    public String createAccessToken(Long userUid) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userUid.toString())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenExpiration))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
