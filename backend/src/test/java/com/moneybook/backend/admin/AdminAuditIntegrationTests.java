package com.moneybook.backend.admin;

import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.admin.service.AdminService;
import com.moneybook.backend.entity.User;
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

    @Test void successfulSensitiveReadWritesAnImmutableAdminAuditSnapshot() {
        TransactionTemplate tx=new TransactionTemplate(txManager);
        Long[] ids=tx.execute(status->{
            User actor=users.save(User.create("operator",null));
            actor.changeSystemRole(SystemRole.SUPER_ADMIN);
            User target=users.save(User.create("ordinary",null));
            return new Long[]{actor.getUserUid(),target.getUserUid()};
        });
        assertNotNull(ids);
        JwtAuthenticationToken token=new JwtAuthenticationToken(Jwt.withTokenValue("jwt").header("alg","none")
                .subject(ids[0].toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());

        tx.executeWithoutResult(status->admins.user(ids[1],token));

        var rows=audits.search(ids[0],com.moneybook.backend.admin.enums.AdminAuditActionType.USER_DETAIL_VIEWED,
                com.moneybook.backend.admin.enums.AdminAuditTargetType.USER,null,null,PageRequest.of(0,10));
        assertEquals(1,rows.getTotalElements());
        var log=rows.getContent().getFirst();
        assertEquals("operator",log.getActorNickname());
        assertEquals(SystemRole.SUPER_ADMIN,log.getActorSystemRole());
        assertEquals(ids[1],log.getTargetUid());
        assertFalse(log.getSummary().contains("ordinary"));
    }
}
