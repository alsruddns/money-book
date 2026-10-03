package com.moneybook.backend.admin;

import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.admin.service.AdminService;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.entity.SystemAdminAuditLog;
import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class AdminAuditIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private AdminService admins;
    @Autowired private AdminAuditRepository audits;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private RefreshSessionRepository sessions;
    @Autowired private UserAuthRepository auths;

    @Test void successfulSensitiveReadWritesAnImmutableAdminAuditSnapshot() {
        TransactionTemplate tx=new TransactionTemplate(txManager);
        Long[] ids=tx.execute(status->{
            User actor=users.save(User.create("operator",null));
            actor.changeSystemRole(SystemRole.SUPER_ADMIN);
            User target=users.save(User.create("ordinary",null));
            auths.save(UserAuth.local(target, "ordinary-login", "$2a$credential-secret"));
            LocalDateTime now = LocalDateTime.now();
            sessions.save(RefreshTokenSession.create(target.getUserUid(), "detail-session", "d".repeat(64),
                    "agent", "127.0.0.1", now, now.plusDays(1)));
            return new Long[]{actor.getUserUid(),target.getUserUid()};
        });
        assertNotNull(ids);
        JwtAuthenticationToken token=new JwtAuthenticationToken(Jwt.withTokenValue("jwt").header("alg","none")
                .subject(ids[0].toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());

        var detail = tx.execute(status -> admins.user(ids[1], token));
        assertEquals(ids[1], detail.userUid());
        assertEquals(java.util.List.of("LOCAL"), detail.authProviders());
        assertEquals(1, detail.activeSessionCount());
        assertFalse(detail.toString().contains("credential-secret"));

        var rows=audits.search(ids[0],null,com.moneybook.backend.admin.enums.AdminAuditActionType.USER_DETAIL_VIEWED,
                com.moneybook.backend.admin.enums.AdminAuditTargetType.USER,null,null,PageRequest.of(0,10));
        assertEquals(1,rows.getTotalElements());
        var log=rows.getContent().getFirst();
        assertEquals("operator",log.getActorNickname());
        assertEquals(SystemRole.SUPER_ADMIN,log.getActorSystemRole());
        assertEquals(ids[1],log.getTargetUid());
        assertFalse(log.getSummary().contains("ordinary"));
    }

    @Test void forcedLogoutRevokesActiveSessionsAndWritesTargetableAudit() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Long[] ids = tx.execute(status -> {
            User actor = users.save(User.create("operator", null));
            actor.changeSystemRole(SystemRole.SUPER_ADMIN);
            User target = users.save(User.create("target", null));
            LocalDateTime now = LocalDateTime.now();
            sessions.save(RefreshTokenSession.create(target.getUserUid(), "active-session", "b".repeat(64),
                    "agent", "127.0.0.1", now, now.plusDays(1)));
            sessions.save(RefreshTokenSession.create(target.getUserUid(), "already-expired", "c".repeat(64),
                    "agent", "127.0.0.1", now.minusDays(2), now.minusDays(1)));
            return new Long[]{actor.getUserUid(), target.getUserUid()};
        });
        JwtAuthenticationToken token = token(ids[0]);

        Integer revoked = tx.execute(status -> admins.revokeAllUserSessions(ids[1], token));
        assertEquals(1, revoked);
        assertTrue(sessions.findActiveByUserUid(ids[1]).isEmpty());
        var audit = audits.search(ids[0], ids[1], AdminAuditActionType.USER_SESSIONS_REVOKED,
                AdminAuditTargetType.USER, null, null, PageRequest.of(0, 10));
        assertEquals(1, audit.getTotalElements());
        assertTrue(audit.getContent().getFirst().getSummary().contains("1"));
    }

    @Test void auditTargetAndDateFiltersAreAppliedBeforePagination() {
        LocalDate today = LocalDate.now();
        audits.save(SystemAdminAuditLog.create(4L, "operator", SystemRole.SUPER_ADMIN,
                AdminAuditActionType.USER_STATUS_CHANGED, AdminAuditTargetType.USER, 11L,
                "status changed", today.atTime(10, 0)));
        audits.save(SystemAdminAuditLog.create(4L, "operator", SystemRole.SUPER_ADMIN,
                AdminAuditActionType.USER_SYSTEM_ROLE_CHANGED, AdminAuditTargetType.USER, 12L,
                "role changed", today.minusDays(2).atTime(10, 0)));
        var page = audits.search(4L, 11L, AdminAuditActionType.USER_STATUS_CHANGED,
                AdminAuditTargetType.USER, today, today, PageRequest.of(0, 1));
        assertEquals(1, page.getTotalElements());
        assertEquals(11L, page.getContent().getFirst().getTargetUid());
    }

    private JwtAuthenticationToken token(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("jwt").header("alg", "none")
                .subject(uid.toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());
    }
}
