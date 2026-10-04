package com.moneybook.backend.config;

import com.moneybook.backend.admin.provider.SystemAdminAuthorizationManager;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.ArrayList;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.config.Customizer;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ObjectProvider<SystemAdminAuthorizationProvider> systemAdminAuthorizationProvider,
            ObjectProvider<UserRepository> userRepositories,
            ObjectProvider<UserAuthRepository> authRepositories,
            JwtDecoder jwtDecoder, Environment environment) throws Exception {
        BearerTokenResolver defaultBearerTokenResolver = new DefaultBearerTokenResolver();
        BearerTokenResolver bearerTokenResolver = request -> {
            if (!isPublicAuthenticationRequest(request)) {
                return defaultBearerTokenResolver.resolve(request);
            }
            try {
                String token = defaultBearerTokenResolver.resolve(request);
                if (token != null) jwtDecoder.decode(token);
                return token;
            } catch (AuthenticationException | JwtException invalidOptionalToken) {
                // Public recovery/signup calls may carry a stale browser token; treat it as absent.
                return null;
            }
        };
        JwtAuthenticationConverter defaultConverter = new JwtAuthenticationConverter();
        Converter<Jwt, AbstractAuthenticationToken> activeUserConverter = jwt -> {
            Long userUid;
            try {
                userUid = Long.valueOf(jwt.getSubject());
            } catch (RuntimeException exception) {
                throw new InvalidBearerTokenException("Invalid access token");
            }
            UserRepository users = userRepositories.getIfAvailable();
            if (users != null) {
                boolean active = users.findById(userUid)
                        .map(user -> user.getStatus() == UserStatus.ACTIVE).orElse(false);
                if (!active) {
                    throw new InvalidBearerTokenException("Invalid access token");
                }
            }
            AbstractAuthenticationToken converted=defaultConverter.convert(jwt);
            UserAuthRepository auths=authRepositories.getIfAvailable();
            if(auths!=null) {
                boolean changeRequired=auths.findLocalByUserUid(userUid).map(com.moneybook.backend.entity.UserAuth::isPasswordChangeRequired).orElse(false);
                if(changeRequired) {
                    var authorities=new ArrayList<>(converted.getAuthorities());
                    authorities.add(new SimpleGrantedAuthority("PASSWORD_CHANGE_REQUIRED"));
                    return new JwtAuthenticationToken(jwt,authorities,converted.getName());
                }
            }
            return converted;
        };
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/signup", "/auth/login", "/auth/refresh",
                        "/auth/logout", "/money-books", "/money-books/**", "/admin/**", "/account",
                        "/account/sessions/**", "/account/security/**", "/board/**",
                        "/auth/email-verifications/**", "/auth/password-recovery/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.httpStrictTransportSecurity(hsts -> {
                        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                            hsts.includeSubDomains(true).maxAgeInSeconds(31536000);
                        } else {
                            hsts.disable();
                        }
                    });
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
                        .requestMatchers(HttpMethod.GET, "/auth/security-questions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/refresh",
                                "/auth/email-verifications/request", "/auth/email-verifications/confirm",
                                "/auth/password-recovery/email/request", "/auth/password-recovery/email/reset",
                                "/auth/password-recovery/security-question/reset",
                                "/auth/password-recovery/recovery-code/reset").permitAll()
                        .requestMatchers("/admin/**")
                        .access(new SystemAdminAuthorizationManager(systemAdminAuthorizationProvider.getIfAvailable()))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, 401, "UNAUTHORIZED", "인증이 필요합니다."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, 403, "FORBIDDEN", "접근 권한이 없습니다."))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(activeUserConverter)))
                .build();
    }

    private static boolean isPublicAuthenticationRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        if ("GET".equals(method) && "/auth/security-questions".equals(path)) return true;
        if (!"POST".equals(method)) return false;
        return "/auth/signup".equals(path)
                || "/auth/login".equals(path)
                || "/auth/refresh".equals(path)
                || "/auth/email-verifications/request".equals(path)
                || "/auth/email-verifications/confirm".equals(path)
                || "/auth/password-recovery/email/request".equals(path)
                || "/auth/password-recovery/email/reset".equals(path)
                || "/auth/password-recovery/security-question/reset".equals(path)
                || "/auth/password-recovery/recovery-code/reset".equals(path);
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
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
