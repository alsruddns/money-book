package com.moneybook.backend.recovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Runs HTTP-level smoke requests against an embedded server with an isolated H2 database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:password-recovery-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "jwt.secret=bW9uZXlib29rLXRlc3Qtc2VjcmV0LWtleS1mb3ItdGVzdC1vbmx5LTMyeQ=="
})
@ActiveProfiles("test")
class PasswordRecoveryHttpSmokeTests {
    @LocalServerPort private int port;
    private final java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
    @MockitoBean private EmailSender emailSender;

    @Test
    void anonymousPasswordRecoveryPathsReachTheApplicationOverHttp() throws Exception {
        assertEquals(200, get("/api/auth/security-questions").statusCode());
        assertEquals(200, get("/api/auth/security-questions", "Bearer invalid-stale-token").statusCode());
        var verificationResponse = post("/api/auth/email-verifications/request",
                "{\"email\":\"smoke@example.test\",\"purpose\":\"SIGNUP\"}");
        assertStatus(200, verificationResponse);
        var code = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(emailSender).sendVerificationCode(org.mockito.ArgumentMatchers.eq("smoke@example.test"),
                code.capture(), org.mockito.ArgumentMatchers.eq("SIGNUP"));
        org.junit.jupiter.api.Assertions.assertFalse(verificationResponse.body().contains(code.getValue()));
        assertStatus(400, post("/api/auth/email-verifications/confirm",
                "{\"verificationUid\":1,\"code\":\"123456\"}"));
        assertStatus(200, post("/api/auth/password-recovery/email/request",
                "{\"loginId\":\"missing-user\",\"email\":\"missing@example.test\"}"));
        assertStatus(400, post("/api/auth/password-recovery/email/reset",
                "{\"verificationToken\":\"invalid-grant\",\"newPassword\":\"NewPassword1!\",\"newPasswordConfirm\":\"NewPassword1!\"}"));
        assertStatus(400, post("/api/auth/password-recovery/security-question/reset",
                "{\"loginId\":\"missing-user\",\"questionCode\":\"FAVORITE_FOOD\",\"answer\":\"x\",\"newPassword\":\"NewPassword1!\",\"newPasswordConfirm\":\"NewPassword1!\"}"));
        assertStatus(400, post("/api/auth/password-recovery/recovery-code/reset",
                "{\"loginId\":\"missing-user\",\"recoveryCode\":\"invalid\",\"newPassword\":\"NewPassword1!\",\"newPasswordConfirm\":\"NewPassword1!\"}"));
    }

    @Test
    void accountSecurityAndAdminResetRemainProtectedOverHttp() throws Exception {
        assertEquals(401, get("/api/account/security").statusCode());
        assertEquals(401, post("/api/admin/users/51/password-reset", "{}").statusCode());
    }

    private java.net.http.HttpResponse<String> get(String path) throws Exception {
        return http.send(java.net.http.HttpRequest.newBuilder(uri(path)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private java.net.http.HttpResponse<String> get(String path, String authorization) throws Exception {
        return http.send(java.net.http.HttpRequest.newBuilder(uri(path)).header("Authorization", authorization)
                        .GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private java.net.http.HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(java.net.http.HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private java.net.URI uri(String path) {
        return java.net.URI.create("http://localhost:" + port + path);
    }

    private void assertStatus(int expected, java.net.http.HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(), "Unexpected HTTP response: " + response.body());
    }
}
