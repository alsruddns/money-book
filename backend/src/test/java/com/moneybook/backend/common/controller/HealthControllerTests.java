package com.moneybook.backend.common.controller;

import com.moneybook.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, HealthControllerTests.TestJwtConfiguration.class})
class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/health").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @TestConfiguration
    static class TestJwtConfiguration {

        @Bean
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new JwtException("Invalid test token");
            };
        }
    }
}
