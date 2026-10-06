package com.moneybook.backend.monitoring;

import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.datasource.url=jdbc:h2:mem:monitoring;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MonitoringActuatorTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private ApplicationContext applicationContext;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserAuthRepository userAuthRepository;

    @BeforeEach
    void allowActiveTestUser() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(User.create("Test User", null)));
        when(userAuthRepository.findLocalByUserUid(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void healthAndProbeEndpointsArePublicAndHideComponentDetails() throws Exception {
        mockMvc.perform(get("/api/actuator/health").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/api/actuator/health/liveness").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/actuator/health/readiness").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
        mockMvc.perform(get("/api/health").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void informationMetricsAndPrometheusRequireAnAccessToken() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(applicationContext.getBeansOfType(MeterRegistry.class).values()
                .stream().anyMatch(registry -> registry.getClass().getName().contains("Prometheus")));
        mockMvc.perform(get("/api/actuator/info").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/actuator/metrics").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/actuator/prometheus").contextPath("/api"))
                .andExpect(status().isUnauthorized());

        String bearer = "Bearer " + accessToken();
        mockMvc.perform(get("/api/actuator/info").contextPath("/api").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("MoneyBook"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("jdbc:"))));
        mockMvc.perform(get("/api/actuator/metrics/jvm.memory.used").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"));
        mockMvc.perform(get("/api/health").contextPath("/api"));
        mockMvc.perform(get("/api/actuator/metrics/http.server.requests").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("http.server.requests"));
        mockMvc.perform(get("/api/actuator/metrics/hikaricp.connections.active").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hikaricp.connections.active"));
        mockMvc.perform(get("/api/actuator/prometheus").contextPath("/api").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }

    private String accessToken() {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("42").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, JwtTokenType.ACCESS.name()).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
