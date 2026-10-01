package com.moneybook.backend.dashboard;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.dashboard.service.DashboardService;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.MoneyBookTransfer;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.transaction.repository.TransactionRepository;
import com.moneybook.backend.transfer.repository.TransferRepository;
import com.moneybook.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class DashboardIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryRepository categories;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransferRepository transfers;
    @Autowired private DashboardService dashboard;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    @Test
    void monthlyTotalsCountsAndBalanceIgnoreTransfersAndOtherMonths() {
        Fixture f = fixture();
        transaction(f, TransactionType.INCOME, "100.25", "2026-10-01", f.income());
        transaction(f, TransactionType.INCOME, "50.75", "2026-10-31", f.income());
        transaction(f, TransactionType.EXPENSE, "30", "2026-10-15", f.expense());
        transaction(f, TransactionType.EXPENSE, "999", "2026-11-01", f.expense());
        transfer(f, "500", "2026-10-20");
        var summary = dashboard.monthly(f.bookUid(), 2026, 10, f.auth());
        assertEquals(0, summary.totalIncome().compareTo(new BigDecimal("151.00")));
        assertEquals(0, summary.totalExpense().compareTo(new BigDecimal("30.00")));
        assertEquals(0, summary.balance().compareTo(new BigDecimal("121.00")));
        assertEquals(3, summary.transactionCount());
        assertEquals(2, summary.incomeCount());
        assertEquals(1, summary.expenseCount());
        var empty = dashboard.monthly(f.bookUid(), 2026, 9, f.auth());
        assertEquals(BigDecimal.ZERO, empty.totalIncome());
        assertEquals(0, empty.transactionCount());
    }

    @Test
    void categoryRatiosAndOrderUseGroupedAmounts() {
        Fixture f = fixture();
        MoneyBookCategory other = categories.save(MoneyBookCategory.create(f.book(), "other",
                TransactionType.EXPENSE, 1));
        transaction(f, TransactionType.EXPENSE, "40", "2026-10-01", f.expense());
        transaction(f, TransactionType.EXPENSE, "30", "2026-10-02", f.expense());
        transactions.save(MoneyBookTransaction.create(f.book(), TransactionType.EXPENSE, new BigDecimal("30"),
                LocalDate.parse("2026-10-03"), other, f.cash(), null));
        var rows = dashboard.categories(f.bookUid(), 2026, 10, TransactionType.EXPENSE, f.auth());
        assertEquals(2, rows.size());
        assertEquals(f.expense().getCategoryUid(), rows.getFirst().categoryUid());
        assertEquals(2, rows.getFirst().transactionCount());
        assertEquals(new BigDecimal("0.7000"), rows.getFirst().ratio());
        assertEquals(new BigDecimal("0.3000"), rows.get(1).ratio());
        assertTrue(dashboard.categories(f.bookUid(), 2026, 9, TransactionType.EXPENSE, f.auth()).isEmpty());
    }

    @Test
    void accountFlowKeepsTransfersSeparateAndIncludesIdleAccountsWithoutNPlusOne() {
        Fixture f = fixture();
        accounts.save(MoneyBookAccount.create(f.book(), "idle", AccountType.ETC, 2));
        transaction(f, TransactionType.INCOME, "100", "2026-10-01", f.income());
        transaction(f, TransactionType.EXPENSE, "25", "2026-10-02", f.expense());
        transfer(f, "40", "2026-10-03");
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var rows = dashboard.accounts(f.bookUid(), 2026, 10, f.auth());
        assertEquals(3, rows.size());
        assertTrue(stats.getPrepareStatementCount() <= 7, "account flow must use fixed query count");
        var cash = rows.stream().filter(row -> row.accountName().equals("cash")).findFirst().orElseThrow();
        var bank = rows.stream().filter(row -> row.accountName().equals("bank")).findFirst().orElseThrow();
        var idle = rows.stream().filter(row -> row.accountName().equals("idle")).findFirst().orElseThrow();
        assertEquals(0, cash.incomeAmount().compareTo(new BigDecimal("100")));
        assertEquals(0, cash.expenseAmount().compareTo(new BigDecimal("25")));
        assertEquals(0, cash.transferOutAmount().compareTo(new BigDecimal("40")));
        assertEquals(BigDecimal.ZERO, cash.transferInAmount());
        assertEquals(0, bank.transferInAmount().compareTo(new BigDecimal("40")));
        assertEquals(BigDecimal.ZERO, bank.incomeAmount());
        assertEquals(BigDecimal.ZERO, idle.expenseAmount());
    }

    @Test
    void ownerAndAcceptedReaderCanViewButPendingRejectedAndNoReadCannot() {
        Fixture f = fixture();
        Long reader = user("reader");
        Long noRead = user("no-read");
        Long pending = user("pending");
        Long rejected = user("rejected");
        member(f.book(), reader, true, true);
        member(f.book(), noRead, false, true);
        member(f.book(), pending, true, false);
        MoneyBookUser rejectedMember = member(f.book(), rejected, true, false);
        rejectedMember.rejectInvitation();
        assertEquals(0, dashboard.monthly(f.bookUid(), 2026, 10, f.auth()).transactionCount());
        assertEquals(0, dashboard.monthly(f.bookUid(), 2026, 10, auth(reader)).transactionCount());
        for (Long uid : new Long[]{noRead, pending, rejected}) {
            assertEquals(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                    assertThrows(BusinessException.class,
                            () -> dashboard.accounts(f.bookUid(), 2026, 10, auth(uid))).getErrorCode());
        }
    }

    private Fixture fixture() {
        Long owner = user("owner");
        MoneyBook book = books.save(MoneyBook.create("book", owner));
        memberships.save(MoneyBookUser.owner(book, owner));
        return new Fixture(owner, book,
                categories.save(MoneyBookCategory.create(book, "salary", TransactionType.INCOME, 0)),
                categories.save(MoneyBookCategory.create(book, "food", TransactionType.EXPENSE, 0)),
                accounts.save(MoneyBookAccount.create(book, "cash", AccountType.CASH, 0)),
                accounts.save(MoneyBookAccount.create(book, "bank", AccountType.BANK, 1)));
    }

    private void transaction(Fixture f, TransactionType type, String amount, String date,
                             MoneyBookCategory category) {
        transactions.save(MoneyBookTransaction.create(f.book(), type, new BigDecimal(amount),
                LocalDate.parse(date), category, f.cash(), null));
    }

    private void transfer(Fixture f, String amount, String date) {
        transfers.save(MoneyBookTransfer.create(f.book(), f.cash(), f.bank(), new BigDecimal(amount),
                LocalDate.parse(date), null));
    }

    private Long user(String nickname) {
        return users.save(User.create(nickname, null)).getUserUid();
    }

    private MoneyBookUser member(MoneyBook book, Long uid, boolean read, boolean accepted) {
        MoneyBookUser member = MoneyBookUser.invite(book, uid, false, false, read, false, false);
        if (accepted) member.acceptInvitation();
        return memberships.save(member);
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private record Fixture(Long owner, MoneyBook book, MoneyBookCategory income,
                           MoneyBookCategory expense, MoneyBookAccount cash, MoneyBookAccount bank) {
        Long bookUid() { return book.getMoneyBookUid(); }
        JwtAuthenticationToken auth() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", owner.toString()).build());
        }
    }
}
