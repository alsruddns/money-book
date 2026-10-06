package com.moneybook.backend.calendar;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.calendar.provider.CalendarProvider;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.Holiday;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.MoneyBookTransfer;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.holiday.client.HolidayApiClient;
import com.moneybook.backend.holiday.repository.HolidayRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:calendar_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class CalendarIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryRepository categories;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransferRepository transfers;
    @Autowired private HolidayRepository holidays;
    @Autowired private CalendarProvider calendar;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @MockitoBean private HolidayApiClient api;

    @Test
    void everyMonthReturnsAllDatesWithCorrectWeekdaysAndWeekends() {
        Fixture f = fixture();
        for (int[] input : new int[][]{{2025, 2, 28}, {2024, 2, 29}, {2026, 4, 30}, {2026, 10, 31}}) {
            var response = calendar.monthly(f.book.getMoneyBookUid(), input[0], input[1], f.auth());
            assertEquals(input[2], response.days().size());
            assertEquals(1, response.days().getFirst().date().getDayOfMonth());
            assertEquals(input[2], response.days().getLast().date().getDayOfMonth());
        }
        var october = calendar.monthly(f.book.getMoneyBookUid(), 2026, 10, f.auth());
        assertEquals(java.time.DayOfWeek.THURSDAY, october.days().getFirst().dayOfWeek());
        assertTrue(october.days().get(2).weekend());
        assertFalse(october.days().getFirst().weekend());
        assertEquals(0, october.days().getFirst().transactionCount());
        assertEquals(BigDecimal.ZERO, october.days().getFirst().expenseAmount());
        assertEquals(ErrorCode.VALIDATION_FAILED, assertThrows(BusinessException.class,
                () -> calendar.monthly(f.book.getMoneyBookUid(), 2026, 13, f.auth())).getErrorCode());
    }

    @Test
    void monthlyGroupsTransactionsTransfersAndGeneratedOccurrencesInFixedQueries() {
        Fixture f = fixture();
        transaction(f, f.income, TransactionType.INCOME, "100", "2026-10-02");
        transaction(f, f.expense, TransactionType.EXPENSE, "30", "2026-10-02");
        transactions.save(MoneyBookTransaction.createRecurring(f.book, TransactionType.EXPENSE,
                new BigDecimal("20"), LocalDate.parse("2026-10-02"), f.expense, f.cash, null, 123L));
        transaction(f, f.expense, TransactionType.EXPENSE, "999", "2026-11-01");
        transfer(f, "50", "2026-10-02");
        holidays.replaceYear(2026, List.of(Holiday.fromKasi(LocalDate.parse("2026-10-03"), "개천절", "01")));
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var days = calendar.monthly(f.book.getMoneyBookUid(), 2026, 10, f.auth()).days();
        assertTrue(stats.getPrepareStatementCount() <= 7, "monthly queries must not grow with day count");
        var second = days.get(1);
        assertEquals(0, second.incomeAmount().compareTo(new BigDecimal("100")));
        assertEquals(0, second.expenseAmount().compareTo(new BigDecimal("50")));
        assertEquals(0, second.transferInAmount().compareTo(new BigDecimal("50")));
        assertEquals(0, second.transferOutAmount().compareTo(new BigDecimal("50")));
        assertEquals(3, second.transactionCount());
        assertEquals(1, second.transferCount());
        assertTrue(second.hasRecurringGeneratedTransaction());
        assertEquals(BigDecimal.ZERO, days.getFirst().expenseAmount());
        assertEquals(BigDecimal.ZERO, days.getLast().expenseAmount());
        var third = days.get(2);
        assertTrue(third.holiday());
        assertTrue(third.weekend());
        assertEquals("개천절", third.holidayName());
    }

    @Test
    void dailyDetailIncludesOnlySelectedDateAndLoadsNamesWithoutNPlusOne() {
        Fixture f = fixture();
        for (int i = 0; i < 8; i++) transaction(f, f.expense, TransactionType.EXPENSE, "1", "2026-10-02");
        transaction(f, f.income, TransactionType.INCOME, "5", "2026-10-03");
        transfer(f, "25", "2026-10-02");
        holidays.replaceYear(2026, List.of(Holiday.fromKasi(LocalDate.parse("2026-10-02"), "휴일", "01")));
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var detail = calendar.daily(f.book.getMoneyBookUid(), LocalDate.parse("2026-10-02"), f.auth());
        assertTrue(stats.getPrepareStatementCount() <= 7, "detail names must use fetch joins");
        assertEquals(8, detail.transactions().size());
        assertEquals(1, detail.transfers().size());
        assertEquals("food", detail.transactions().getFirst().categoryName());
        assertEquals("cash", detail.transactions().getFirst().accountName());
        assertEquals("bank", detail.transfers().getFirst().toAccountName());
        assertTrue(detail.holiday());
        assertEquals("휴일", detail.holidayName());
    }

    @Test
    void readerCanViewButMemberWithoutReadCannot() {
        Fixture f = fixture();
        Long readerUid = users.save(User.create("reader", null)).getUserUid();
        MoneyBookUser reader = MoneyBookUser.invite(f.book, readerUid, false, false, true, false, false);
        reader.acceptInvitation();
        memberships.save(reader);
        assertEquals(31, calendar.monthly(f.book.getMoneyBookUid(), 2026, 10, auth(readerUid)).days().size());
        Long deniedUid = users.save(User.create("denied", null)).getUserUid();
        MoneyBookUser denied = MoneyBookUser.invite(f.book, deniedUid, false, false, false, true, false);
        denied.acceptInvitation();
        memberships.save(denied);
        assertEquals(ErrorCode.MONEY_BOOK_READ_FORBIDDEN, assertThrows(BusinessException.class,
                () -> calendar.daily(f.book.getMoneyBookUid(), LocalDate.parse("2026-10-02"), auth(deniedUid)))
                .getErrorCode());
    }

    private Fixture fixture() {
        Long owner = users.save(User.create("owner", null)).getUserUid();
        MoneyBook book = books.save(MoneyBook.create("book", owner));
        memberships.save(MoneyBookUser.owner(book, owner));
        return new Fixture(owner, book,
                categories.save(MoneyBookCategory.create(book, "salary", TransactionType.INCOME, 0)),
                categories.save(MoneyBookCategory.create(book, "food", TransactionType.EXPENSE, 0)),
                accounts.save(MoneyBookAccount.create(book, "cash", AccountType.CASH, 0)),
                accounts.save(MoneyBookAccount.create(book, "bank", AccountType.BANK, 1)));
    }

    private void transaction(Fixture f, MoneyBookCategory category, TransactionType type, String amount, String date) {
        transactions.save(MoneyBookTransaction.create(f.book, type, new BigDecimal(amount),
                LocalDate.parse(date), category, f.cash, null));
    }

    private void transfer(Fixture f, String amount, String date) {
        transfers.save(MoneyBookTransfer.create(f.book, f.cash, f.bank, new BigDecimal(amount),
                LocalDate.parse(date), null));
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private record Fixture(Long owner, MoneyBook book, MoneyBookCategory income, MoneyBookCategory expense,
                           MoneyBookAccount cash, MoneyBookAccount bank) {
        JwtAuthenticationToken auth() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", owner.toString()).build());
        }
    }
}
