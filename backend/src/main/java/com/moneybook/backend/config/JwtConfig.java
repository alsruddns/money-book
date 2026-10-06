package com.moneybook.backend.config;

import com.moneybook.backend.enums.JwtTokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class JwtConfig {

    /** Requires a Base64 encoded key of at least 256 bits for HS256. */
    @Bean
    public SecretKey jwtSecretKey(@Value("${jwt.secret}") String configuredSecret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT_SECRET must be a Base64 encoded key", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 decoded bytes");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(List.of(
                new JwtClaimValidator<String>(JwtTokenType.CLAIM_NAME,
                        JwtTokenType.ACCESS.name()::equals))));
        return decoder;
    }
}
