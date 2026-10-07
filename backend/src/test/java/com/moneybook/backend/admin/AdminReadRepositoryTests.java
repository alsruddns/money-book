package com.moneybook.backend.admin;

import com.moneybook.backend.admin.repository.AdminReadRepository;
import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_read;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class AdminReadRepositoryTests {
    @Autowired private AdminReadRepository reads;
    @Autowired private UserRepository users;
    @Autowired private UserAuthRepository auths;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private ActivityRepository activities;
    @Autowired private EntityManager em;

    @Test void listsAndDetailsUsePagedOperationalProjectionsAndAggregates() {
        User user=users.save(User.create("owner",null));
        auths.save(UserAuth.local(user,"owner@example.test","$2a$test"));
        MoneyBook book=books.save(MoneyBook.create("Family Book",user.getUserUid()));
        memberships.save(MoneyBookUser.owner(book,user.getUserUid()));
        MoneyBookCategory salary = MoneyBookCategory.create(book, "Salary", TransactionType.INCOME, 0);
        MoneyBookAccount cash = MoneyBookAccount.create(book, "Cash", AccountType.CASH, 0);
        MoneyBookAccount bank = MoneyBookAccount.create(book, "Bank", AccountType.BANK, 1);
        em.persist(salary);
        em.persist(cash);
        em.persist(bank);
        em.flush();
        em.persist(MoneyBookTransaction.create(book, TransactionType.INCOME, new BigDecimal("100"),
                LocalDate.now(), salary, cash, null));
        em.persist(MoneyBookTransfer.create(book, cash, bank, new BigDecimal("10"), LocalDate.now(), null));
        em.persist(RecurringTransaction.create(book, TransactionType.INCOME, new BigDecimal("100"), salary,
                cash, RecurringFrequency.MONTHLY, 1, null, LocalDate.now(), null, null));
        em.flush();

        var userPage=reads.users("owner@",null,SystemRole.USER,PageRequest.of(0,20));
        assertEquals(1,userPage.getTotalElements());
        assertEquals("owner@example.test",userPage.getContent().getFirst().loginId());
        var userDetail = reads.user(user.getUserUid()).orElseThrow();
        assertEquals(0,userDetail.joinedMoneyBookCount());
        assertEquals(java.util.Set.of(com.moneybook.backend.enums.AuthProvider.LOCAL), reads.authProviders(user.getUserUid()));
        var bookPage=reads.moneyBooks("family",user.getUserUid(),PageRequest.of(0,20));
        assertEquals(1,bookPage.getTotalElements());
        assertEquals("owner",bookPage.getContent().getFirst().ownerNickname());
        assertEquals(1,bookPage.getContent().getFirst().memberCount());
        var bookDetail = reads.moneyBook(book.getMoneyBookUid()).orElseThrow();
        assertEquals(1, bookDetail.memberCount());
        assertEquals(1, bookDetail.adminMemberCount());
        assertEquals(1, bookDetail.transactionCount());
        assertEquals(1, bookDetail.incomeTransactionCount());
        assertEquals(0, bookDetail.expenseTransactionCount());
        assertEquals(1, bookDetail.transferCount());
        assertEquals(1, bookDetail.recurringRuleCount());
        assertEquals(1, bookDetail.categoryCount());
        assertEquals(2, bookDetail.accountCount());
        assertEquals(1,reads.overview().totalMoneyBooks());
        assertEquals(0,activities.search(null,null,null,null,ActivityType.MONEY_BOOK_CREATED,null,
                PageRequest.of(0,20,org.springframework.data.domain.Sort.by("occurredAt"))).getTotalElements());
    }

    @Test void overviewCountsStatusesGrowthMembershipActivityAndSessions() {
        LocalDateTime now = LocalDateTime.now();
        User owner = users.save(User.create("active-owner", null));
        owner.changeSystemRole(SystemRole.SYSTEM_ADMIN);
        User blocked = users.save(User.create("blocked", null));
        blocked.changeStatus(UserStatus.BLOCKED);
        User withdrawn = users.save(User.create("withdrawn", null));
        withdrawn.changeStatus(UserStatus.WITHDRAWN);
        MoneyBook book = books.save(MoneyBook.create("Operational", owner.getUserUid()));
        memberships.save(MoneyBookUser.owner(book, owner.getUserUid()));
        activities.save(MoneyBookActivity.create(book.getMoneyBookUid(), owner.getUserUid(), owner.getNickname(),
                ActivityType.MONEY_BOOK_CREATED, ActivityTargetType.MONEY_BOOK, book.getMoneyBookUid(),
                "created", null, now));
        em.persist(RefreshTokenSession.create(owner.getUserUid(), "session-key", "a".repeat(64),
                "test-agent", "127.0.0.1", now, now.plusDays(1)));
        em.flush();

        var overview = reads.overview();
        assertEquals(3, overview.totalUsers());
        assertEquals(1, overview.activeUsers());
        assertEquals(1, overview.blockedUsers());
        assertEquals(1, overview.withdrawnUsers());
        assertEquals(1, overview.systemAdminCount());
        assertEquals(1, overview.totalMemberships());
        assertEquals(1, overview.activeMemberCount());
        assertEquals(1, overview.activeSessionCount());
        assertEquals(1, overview.activitiesToday());
        assertEquals(1, overview.last30DaysActivityCount());
        assertEquals(3, overview.todayNewUsers());
        assertEquals(3, overview.last7DaysNewUsers());
        assertEquals(3, overview.last30DaysNewUsers());
        assertEquals(1, overview.todayNewMoneyBooks());
        assertEquals(1, overview.last7DaysNewMoneyBooks());
        assertEquals(1, overview.last30DaysNewMoneyBooks());
        assertEquals(1, overview.last7DaysActivityCount());
    }

    @Test void adminBookMembersArePagedAndIncludePermissionStateWithoutCredentialData() {
        User owner = users.save(User.create("book-owner", null));
        User member = users.save(User.create("book-member", null));
        auths.save(UserAuth.local(member, "sensitive-login", "$2a$never-return"));
        MoneyBook book = books.save(MoneyBook.create("Members", owner.getUserUid()));
        memberships.save(MoneyBookUser.owner(book, owner.getUserUid()));
        MoneyBookUser invited = MoneyBookUser.invite(book, member.getUserUid(), false, true, true, false, false);
        invited.acceptInvitation();
        memberships.save(invited);
        var page = reads.moneyBookMembers(book.getMoneyBookUid(), PageRequest.of(0, 1));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().getFirst().isOwner());
        assertEquals(member.getUserUid(), reads.moneyBookMembers(book.getMoneyBookUid(), PageRequest.of(1, 1))
                .getContent().getFirst().userUid());
    }
}
