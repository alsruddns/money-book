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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.config.Customizer;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

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
                        "/auth/logout", "/money-books", "/money-books/**", "/admin/**", "/account",
                        "/account/sessions/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)
                            .maxAgeInSeconds(31536000));
                    headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                            "camera=(), microphone=(), geolocation=()"));
                    headers.addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"));
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, 401, "UNAUTHORIZED", "인증이 필요합니다."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, 403, "FORBIDDEN", "접근 권한이 없습니다.")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/admin/**")
                        .access(new SystemAdminAuthorizationManager(systemAdminAuthorizationProvider.getIfAvailable()))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, 401, "UNAUTHORIZED", "인증이 필요합니다."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, 403, "FORBIDDEN", "접근 권한이 없습니다."))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(activeUserConverter)))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeSecurityError(jakarta.servlet.http.HttpServletResponse response,
                                           int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
