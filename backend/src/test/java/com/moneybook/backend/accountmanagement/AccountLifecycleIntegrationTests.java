package com.moneybook.backend.accountmanagement;

import com.moneybook.backend.accountmanagement.dto.AccountWithdrawalRequest;
import com.moneybook.backend.accountmanagement.service.AccountManagementService;
import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.auth.dto.LoginRequest;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshRequest;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.auth.token.JwtTokenProvider;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookActivity;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.dto.TransferMoneyBookOwnerRequest;
import com.moneybook.backend.moneybook.service.MoneyBookService;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:account_lifecycle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class AccountLifecycleIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private UserAuthRepository auths;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private MoneyBookService moneyBooks;
    @Autowired private AccountManagementService accounts;
    @Autowired private ActivityRepository activities;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private AuthService authService;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mvc;

    @Test
    void ownershipTransferRequiresOwnerAndAcceptedActiveMemberAndGrantsAllPermissions() {
        Fixture f = fixture();
        var response = moneyBooks.transferOwner(f.book().getMoneyBookUid(),
                new TransferMoneyBookOwnerRequest(f.target().getUserUid()), auth(f.owner().getUserUid()));
        assertEquals(f.owner().getUserUid(), response.previousOwnerUserUid());
        assertEquals(f.target().getUserUid(), response.ownerUserUid());
        MoneyBook persisted = books.findById(f.book().getMoneyBookUid()).orElseThrow();
        assertEquals(f.target().getUserUid(), persisted.getOwnerUserUid());
        MoneyBookUser promoted = memberships.findByMoneyBookUidAndUserUid(
                persisted.getMoneyBookUid(), f.target().getUserUid()).orElseThrow();
        assertEquals(InvitationStatus.ACCEPTED, promoted.getInvitationStatus());
        assertTrue(promoted.isAdmin() && promoted.isCanCreate() && promoted.isCanRead()
                && promoted.isCanUpdate() && promoted.isCanDelete());
        assertTrue(memberships.findByMoneyBookUidAndUserUid(persisted.getMoneyBookUid(),
                f.owner().getUserUid()).isPresent(), "former owner membership remains");
        assertEquals(1, activityCount(persisted.getMoneyBookUid(), ActivityType.OWNER_TRANSFERRED));
        assertEquals("{\"previousOwnerUserUid\":" + f.owner().getUserUid()
                        + ",\"newOwnerUserUid\":" + f.target().getUserUid() + "}",
                entityManager.createQuery("select a.metadataJson from MoneyBookActivity a "
                                + "where a.moneyBookUid = :bookUid and a.activityType = :type", String.class)
                        .setParameter("bookUid", persisted.getMoneyBookUid())
                        .setParameter("type", ActivityType.OWNER_TRANSFERRED).getSingleResult());
    }

    @Test
    void ownershipTransferRejectsNonOwnerSelfPendingAndBlockedTargets() {
        Fixture f = fixture();
        User admin = user("admin");
        MoneyBookUser adminMembership = MoneyBookUser.invite(
                f.book(), admin.getUserUid(), true, true, true, true, true);
        adminMembership.acceptInvitation();
        memberships.save(adminMembership);
        fail(ErrorCode.MONEY_BOOK_OWNER_TRANSFER_FORBIDDEN, () -> moneyBooks.transferOwner(
                f.book().getMoneyBookUid(), new TransferMoneyBookOwnerRequest(f.target().getUserUid()),
                auth(admin.getUserUid())));
        fail(ErrorCode.MONEY_BOOK_OWNER_TRANSFER_TO_SELF, () -> moneyBooks.transferOwner(
                f.book().getMoneyBookUid(), new TransferMoneyBookOwnerRequest(f.owner().getUserUid()),
                auth(f.owner().getUserUid())));
        fail(ErrorCode.USER_NOT_FOUND, () -> moneyBooks.transferOwner(f.book().getMoneyBookUid(),
                new TransferMoneyBookOwnerRequest(99999999L), auth(f.owner().getUserUid())));
        User pending = user("pending");
        memberships.save(MoneyBookUser.invite(f.book(), pending.getUserUid(), false, false, false, false, false));
        fail(ErrorCode.MONEY_BOOK_MEMBER_NOT_ACCEPTED, () -> moneyBooks.transferOwner(
                f.book().getMoneyBookUid(), new TransferMoneyBookOwnerRequest(pending.getUserUid()),
                auth(f.owner().getUserUid())));
        f.target().changeStatus(UserStatus.BLOCKED);
        users.save(f.target());
        fail(ErrorCode.USER_INACTIVE, () -> moneyBooks.transferOwner(
                f.book().getMoneyBookUid(), new TransferMoneyBookOwnerRequest(f.target().getUserUid()),
                auth(f.owner().getUserUid())));
        assertEquals(f.owner().getUserUid(), books.findById(f.book().getMoneyBookUid()).orElseThrow().getOwnerUserUid());
    }

    @Test
    void transferRollbackLeavesOwnerAndActivityUnchanged() {
        Fixture f = fixture();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            moneyBooks.transferOwner(f.book().getMoneyBookUid(),
                    new TransferMoneyBookOwnerRequest(f.target().getUserUid()), auth(f.owner().getUserUid()));
            throw new IllegalStateException("force rollback");
        }));
        assertEquals(f.owner().getUserUid(), books.findById(f.book().getMoneyBookUid()).orElseThrow().getOwnerUserUid());
        assertFalse(memberships.findByMoneyBookUidAndUserUid(f.book().getMoneyBookUid(),
                f.target().getUserUid()).orElseThrow().isAdmin());
        assertEquals(0, activityCount(f.book().getMoneyBookUid(), ActivityType.OWNER_TRANSFERRED));
    }

    @Test
    void withdrawalRequiresPasswordAndNoOwnedBookThenCleansMembershipAndCredentials() {
        User user = user("account-user");
        auths.save(UserAuth.local(user, "reusable-login", passwordEncoder.encode("current-password")));
        MoneyBook joined = books.save(MoneyBook.create("joined", user("book-owner").getUserUid()));
        MoneyBookUser accepted = MoneyBookUser.invite(joined, user.getUserUid(), false, true, true, false, false);
        accepted.acceptInvitation();
        memberships.save(accepted);
        MoneyBook pendingBook = books.save(MoneyBook.create("pending", user("other-owner").getUserUid()));
        memberships.save(MoneyBookUser.invite(pendingBook, user.getUserUid(), false, false, false, false, false));
        activities.save(MoneyBookActivity.create(joined.getMoneyBookUid(), user.getUserUid(), "account-user",
                ActivityType.MEMBER_INVITATION_ACCEPTED, ActivityTargetType.MEMBER, 1L,
                "historical snapshot", null, LocalDateTime.now()));

        fail(ErrorCode.INVALID_CURRENT_PASSWORD, () -> accounts.withdraw(auth(user.getUserUid()),
                new AccountWithdrawalRequest("wrong-password")));
        assertEquals(UserStatus.ACTIVE, users.findById(user.getUserUid()).orElseThrow().getStatus());
        LoginResponse login = authService.login(new LoginRequest("reusable-login", "current-password"),
                "integration-test", "127.0.0.1");
        accounts.withdraw(auth(user.getUserUid()), new AccountWithdrawalRequest("current-password"));

        User withdrawn = users.findById(user.getUserUid()).orElseThrow();
        assertEquals(UserStatus.WITHDRAWN, withdrawn.getStatus());
        assertEquals("탈퇴회원-" + user.getUserUid(), withdrawn.getNickname());
        assertTrue(auths.findByUserUid(user.getUserUid()).isEmpty());
        assertTrue(memberships.findByMoneyBookUidAndUserUid(joined.getMoneyBookUid(), user.getUserUid()).isEmpty());
        assertTrue(memberships.findByMoneyBookUidAndUserUid(pendingBook.getMoneyBookUid(), user.getUserUid()).isEmpty());
        assertEquals("account-user", entityManager.createQuery("select a.actorNickname from MoneyBookActivity a "
                + "where a.moneyBookUid = :bookUid", String.class).setParameter("bookUid", joined.getMoneyBookUid())
                .getSingleResult());
        assertEquals(ErrorCode.LOGIN_FAILED, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("reusable-login", "current-password"),
                        "integration-test", "127.0.0.1")).getErrorCode());
        fail(ErrorCode.INVALID_REFRESH_TOKEN, () -> authService.refresh(new RefreshRequest(login.refreshToken())));
        assertTrue(auths.findByLocalLoginId("reusable-login").isEmpty());
        User replacement = user("replacement");
        auths.save(UserAuth.local(replacement, "reusable-login", passwordEncoder.encode("new-password")));
        assertEquals(AuthProvider.LOCAL, auths.findByLocalLoginId("reusable-login").orElseThrow().getProvider());
    }

    @Test
    void ownershipAndSuperAdminPoliciesProtectWithdrawal() {
        User owner = user("withdraw-owner");
        auths.save(UserAuth.local(owner, "owner-login", passwordEncoder.encode("pw")));
        MoneyBook owned = books.save(MoneyBook.create("owned", owner.getUserUid()));
        memberships.save(MoneyBookUser.owner(owned, owner.getUserUid()));
        fail(ErrorCode.OWNED_MONEY_BOOK_EXISTS,
                () -> accounts.withdraw(auth(owner.getUserUid()), new AccountWithdrawalRequest("pw")));
        assertTrue(memberships.findByMoneyBookUidAndUserUid(owned.getMoneyBookUid(), owner.getUserUid()).isPresent());

        User superAdmin = user("super-admin");
        superAdmin.changeSystemRole(SystemRole.SUPER_ADMIN);
        users.save(superAdmin);
        auths.save(UserAuth.local(superAdmin, "super-login", passwordEncoder.encode("pw")));
        fail(ErrorCode.SUPER_ADMIN_WITHDRAWAL_FORBIDDEN,
                () -> accounts.withdraw(auth(superAdmin.getUserUid()), new AccountWithdrawalRequest("pw")));

        User systemAdmin = user("system-admin");
        systemAdmin.changeSystemRole(SystemRole.SYSTEM_ADMIN);
        users.save(systemAdmin);
        auths.save(UserAuth.local(systemAdmin, "system-login", passwordEncoder.encode("pw")));
        accounts.withdraw(auth(systemAdmin.getUserUid()), new AccountWithdrawalRequest("pw"));
        assertEquals(UserStatus.WITHDRAWN, users.findById(systemAdmin.getUserUid()).orElseThrow().getStatus());
    }

    @Test
    void withdrawalRollsBackStatusCredentialAndMembershipTogether() {
        User user = user("rollback-user");
        auths.save(UserAuth.local(user, "rollback-login", passwordEncoder.encode("pw")));
        MoneyBook book = books.save(MoneyBook.create("rollback-book", user("rollback-owner").getUserUid()));
        MoneyBookUser member = MoneyBookUser.invite(book, user.getUserUid(), false, false, true, false, false);
        member.acceptInvitation();
        memberships.save(member);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            accounts.withdraw(auth(user.getUserUid()), new AccountWithdrawalRequest("pw"));
            throw new IllegalStateException("force rollback");
        }));
        assertEquals(UserStatus.ACTIVE, users.findById(user.getUserUid()).orElseThrow().getStatus());
        assertTrue(auths.findByLocalLoginId("rollback-login").isPresent());
        assertTrue(memberships.findByMoneyBookUidAndUserUid(book.getMoneyBookUid(), user.getUserUid()).isPresent());
    }

    @Test
    void withdrawnAccessTokenIsRejectedOnEveryProtectedRequest() throws Exception {
        User user = user("jwt-user");
        auths.save(UserAuth.local(user, "jwt-login", passwordEncoder.encode("pw")));
        String token = tokenProvider.createAccessToken(user.getUserUid());
        mvc.perform(get("/api/auth/me").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        accounts.withdraw(auth(user.getUserUid()), new AccountWithdrawalRequest("pw"));
        mvc.perform(get("/api/auth/me").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/me").contextPath("/api").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private Fixture fixture() {
        User owner = user("owner-transfer");
        User target = user("target-transfer");
        MoneyBook book = books.save(MoneyBook.create("transfer book", owner.getUserUid()));
        memberships.save(MoneyBookUser.owner(book, owner.getUserUid()));
        MoneyBookUser member = MoneyBookUser.invite(book, target.getUserUid(), false, false, false, false, false);
        member.acceptInvitation();
        memberships.save(member);
        return new Fixture(owner, target, book);
    }

    private User user(String nickname) {
        return users.save(User.create(nickname, null));
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private long activityCount(Long bookUid, ActivityType type) {
        return entityManager.createQuery("select count(a) from MoneyBookActivity a "
                        + "where a.moneyBookUid = :bookUid and a.activityType = :type", Long.class)
                .setParameter("bookUid", bookUid).setParameter("type", type).getSingleResult();
    }

    private void fail(ErrorCode expected, Runnable call) {
        assertEquals(expected, assertThrows(BusinessException.class, call::run).getErrorCode());
    }

    private record Fixture(User owner, User target, MoneyBook book) { }
}
