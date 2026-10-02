package com.moneybook.backend.config;

import com.moneybook.backend.admin.provider.SystemAdminAuthorizationManager;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ObjectProvider<SystemAdminAuthorizationProvider> systemAdminAuthorizationProvider,
            ObjectProvider<UserRepository> userRepositories) throws Exception {
        JwtAuthenticationConverter defaultConverter = new JwtAuthenticationConverter();
        Converter<Jwt, AbstractAuthenticationToken> activeUserConverter = jwt -> {
            UserRepository users = userRepositories.getIfAvailable();
            if (users != null) {
                Long userUid;
                try {
                    userUid = Long.valueOf(jwt.getSubject());
                } catch (RuntimeException exception) {
                    throw new InvalidBearerTokenException("Invalid access token");
                }
                boolean active = users.findById(userUid)
                        .map(user -> user.getStatus() == UserStatus.ACTIVE).orElse(false);
                if (!active) {
                    throw new InvalidBearerTokenException("Invalid access token");
                }
            }
            return defaultConverter.convert(jwt);
        };
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/signup", "/auth/login", "/auth/refresh",
                        "/money-books", "/money-books/**", "/admin/**", "/account"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/admin/**")
                        .access(new SystemAdminAuthorizationManager(systemAdminAuthorizationProvider.getIfAvailable()))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(activeUserConverter)))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
