package com.moneybook.backend.recurring;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.recurring.dto.CreateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionActiveRequest;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionRequest;
import com.moneybook.backend.recurring.service.RecurringTransactionService;
import com.moneybook.backend.transaction.service.TransactionService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:recurring_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class RecurringIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryRepository categories;
    @Autowired private AccountRepository accounts;
    @Autowired private RecurringTransactionService recurring;
    @Autowired private TransactionService transactions;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void monthlyAndWeeklyRulesCanBeCreatedListedUpdatedToggledAndDeleted() {
        Fixture f = fixture();
        var monthly = recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.MONTHLY, 25, null, "2026-01-01", null), f.auth());
        var weekly = recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.WEEKLY, null, 1, "2026-01-01", null), f.auth());
        assertTrue(monthly.isActive());
        assertEquals("food", monthly.categoryName());
        assertEquals("cash", monthly.accountName());
        assertEquals(RecurringFrequency.WEEKLY, weekly.frequency());
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertEquals(2, recurring.list(f.bookUid(), f.auth()).size());
        assertTrue(statistics.getPrepareStatementCount() <= 3, "rule names must not cause N+1");
        var updated = recurring.update(f.bookUid(), monthly.recurringTransactionUid(),
                new UpdateRecurringTransactionRequest(TransactionType.EXPENSE, new BigDecimal("20"),
                        f.expense().getCategoryUid(), f.account().getAccountUid(), RecurringFrequency.MONTHLY,
                        31, null, LocalDate.parse("2026-01-01"), null, "new"), f.auth());
        assertEquals(31, updated.dayOfMonth());
        assertEquals("new", updated.memo());
        assertFalse(recurring.changeActive(f.bookUid(), monthly.recurringTransactionUid(),
                new UpdateRecurringTransactionActiveRequest(false), f.auth()).isActive());
        assertTrue(recurring.changeActive(f.bookUid(), monthly.recurringTransactionUid(),
                new UpdateRecurringTransactionActiveRequest(true), f.auth()).isActive());
        recurring.delete(f.bookUid(), weekly.recurringTransactionUid(), f.auth());
        assertEquals(1, recurring.list(f.bookUid(), f.auth()).size());
    }

    @Test
    void rejectsInvalidScheduleDateTypeAndForeignReferences() {
        Fixture f = fixture();
        MoneyBook other = book(f.ownerUid());
        MoneyBookCategory foreignCategory = category(other, "foreign", TransactionType.EXPENSE);
        MoneyBookAccount foreignAccount = account(other, "foreign");
        MoneyBookCategory income = category(f.book(), "salary", TransactionType.INCOME);
        error(ErrorCode.INVALID_RECURRING_DAY, () -> recurring.create(f.bookUid(),
                request(f.expense(), f.account(), RecurringFrequency.MONTHLY, 0, null,
                        "2026-01-01", null), f.auth()));
        error(ErrorCode.INVALID_RECURRING_DAY, () -> recurring.create(f.bookUid(),
                request(f.expense(), f.account(), RecurringFrequency.WEEKLY, null, 8,
                        "2026-01-01", null), f.auth()));
        error(ErrorCode.INVALID_RECURRING_DAY, () -> recurring.create(f.bookUid(),
                request(f.expense(), f.account(), RecurringFrequency.MONTHLY, 10, 1,
                        "2026-01-01", null), f.auth()));
        error(ErrorCode.INVALID_RECURRING_DATE_RANGE, () -> recurring.create(f.bookUid(),
                request(f.expense(), f.account(), RecurringFrequency.MONTHLY, 10, null,
                        "2026-02-01", "2026-01-01"), f.auth()));
        error(ErrorCode.CATEGORY_NOT_IN_MONEY_BOOK, () -> recurring.create(f.bookUid(),
                request(foreignCategory, f.account(), RecurringFrequency.MONTHLY, 10, null,
                        "2026-01-01", null), f.auth()));
        error(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK, () -> recurring.create(f.bookUid(),
                request(f.expense(), foreignAccount, RecurringFrequency.MONTHLY, 10, null,
                        "2026-01-01", null), f.auth()));
        error(ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH, () -> recurring.create(f.bookUid(),
                request(income, f.account(), RecurringFrequency.MONTHLY, 10, null,
                        "2026-01-01", null), f.auth()));
        var created = recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.MONTHLY, 10, null, "2026-01-01", null), f.auth());
        error(ErrorCode.INVALID_RECURRING_DAY, () -> recurring.update(f.bookUid(),
                created.recurringTransactionUid(), new UpdateRecurringTransactionRequest(
                        TransactionType.EXPENSE, BigDecimal.ONE, f.expense().getCategoryUid(),
                        f.account().getAccountUid(), RecurringFrequency.MONTHLY, 32, null,
                        LocalDate.parse("2026-01-01"), null, null), f.auth()));
        error(ErrorCode.RECURRING_TRANSACTION_NOT_FOUND, () -> recurring.changeActive(
                other.getMoneyBookUid(), created.recurringTransactionUid(),
                new UpdateRecurringTransactionActiveRequest(false), f.auth()));
    }

    @Test
    void generateMissingMonthlyOccurrencesAndRepeatIsIdempotent() {
        Fixture f = fixture();
        var rule = recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.MONTHLY, 25, null, "2026-01-01", null), f.auth());
        assertEquals(0, generate(f, "2025-12-31"));
        assertEquals(4, generate(f, "2026-04-30"));
        assertEquals(0, generate(f, "2026-04-30"));
        assertEquals(List.of(LocalDate.parse("2026-04-25")),
                transactions.list(f.bookUid(), 2026, 4, f.auth()).stream()
                        .map(row -> row.transactionDate()).toList());
        assertEquals(1, transactions.list(f.bookUid(), 2026, 2, f.auth()).size());
        assertEquals(LocalDate.parse("2026-04-25"),
                recurring.list(f.bookUid(), f.auth()).getFirst().lastGeneratedDate());
        recurring.delete(f.bookUid(), rule.recurringTransactionUid(), f.auth());
        assertTrue(recurring.list(f.bookUid(), f.auth()).isEmpty());
        assertEquals(1, transactions.list(f.bookUid(), 2026, 4, f.auth()).size());
    }

    @Test
    void monthlyThirtyFirstClampsToFebruaryEndIncludingLeapYear() {
        Fixture f = fixture();
        recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.MONTHLY, 31, null, "2026-01-01", "2028-02-29"), f.auth());
        assertEquals(26, generate(f, "2028-02-29"));
        assertEquals(LocalDate.parse("2026-02-28"),
                transactions.list(f.bookUid(), 2026, 2, f.auth()).getFirst().transactionDate());
        assertEquals(LocalDate.parse("2028-02-29"),
                transactions.list(f.bookUid(), 2028, 2, f.auth()).getFirst().transactionDate());
        assertEquals(0, generate(f, "2028-03-31"));
    }

    @Test
    void weeklyScheduleRespectsStartEndAndInactiveState() {
        Fixture f = fixture();
        var rule = recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.WEEKLY, null, 1, "2026-10-06", "2026-10-20"), f.auth());
        assertEquals(2, generate(f, "2026-10-31"));
        assertEquals(List.of(LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-12")),
                transactions.list(f.bookUid(), 2026, 10, f.auth()).stream()
                        .map(row -> row.transactionDate()).toList());
        recurring.changeActive(f.bookUid(), rule.recurringTransactionUid(),
                new UpdateRecurringTransactionActiveRequest(false), f.auth());
        assertEquals(0, generate(f, "2026-11-30"));
    }

    @Test
    void generationLimitRejectsBeforeWritingAnyTransaction() {
        Fixture f = fixture();
        recurring.create(f.bookUid(), request(f.expense(), f.account(),
                RecurringFrequency.WEEKLY, null, 1, "2000-01-01", null), f.auth());
        error(ErrorCode.RECURRING_GENERATION_LIMIT_EXCEEDED,
                () -> generate(f, "2010-01-01"));
        assertTrue(transactions.list(f.bookUid(), 2000, 1, f.auth()).isEmpty());
    }

    @Test
    void createReadUpdateDeletePermissionsRejectPendingAndRejectedMembers() {
        Fixture f = fixture();
        Long creator = user("creator");
        Long reader = user("reader");
        Long updater = user("updater");
        Long deleter = user("deleter");
        Long pending = user("pending");
        Long rejected = user("rejected");
        member(f.book(), creator, true, false, false, false, true);
        member(f.book(), reader, false, true, false, false, true);
        member(f.book(), updater, false, false, true, false, true);
        member(f.book(), deleter, false, false, false, true, true);
        member(f.book(), pending, true, true, true, true, false);
        MoneyBookUser rejectedMember = member(f.book(), rejected, true, true, true, true, false);
        rejectedMember.rejectInvitation();
        var request = request(f.expense(), f.account(), RecurringFrequency.MONTHLY, 1, null,
                "2026-01-01", null);
        var rule = recurring.create(f.bookUid(), request, auth(creator));
        assertEquals(1, recurring.list(f.bookUid(), auth(reader)).size());
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN,
                () -> recurring.create(f.bookUid(), request, auth(reader)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> recurring.list(f.bookUid(), auth(creator)));
        recurring.changeActive(f.bookUid(), rule.recurringTransactionUid(),
                new UpdateRecurringTransactionActiveRequest(false), auth(updater));
        error(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN,
                () -> recurring.changeActive(f.bookUid(), rule.recurringTransactionUid(),
                        new UpdateRecurringTransactionActiveRequest(true), auth(reader)));
        error(ErrorCode.MONEY_BOOK_DELETE_FORBIDDEN,
                () -> recurring.delete(f.bookUid(), rule.recurringTransactionUid(), auth(reader)));
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN,
                () -> recurring.generate(f.bookUid(), new GenerateRecurringTransactionRequest(
                        LocalDate.parse("2026-01-31")), auth(pending)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> recurring.list(f.bookUid(), auth(rejected)));
        recurring.delete(f.bookUid(), rule.recurringTransactionUid(), auth(deleter));
    }

    private int generate(Fixture f, String date) {
        return recurring.generate(f.bookUid(), new GenerateRecurringTransactionRequest(LocalDate.parse(date)),
                f.auth()).generatedCount();
    }

    private Fixture fixture() {
        Long ownerUid = user("owner");
        MoneyBook book = book(ownerUid);
        return new Fixture(ownerUid, book,
                category(book, "food", TransactionType.EXPENSE), account(book, "cash"));
    }

    private Long user(String nickname) {
        return users.save(User.create(nickname, null)).getUserUid();
    }

    private MoneyBook book(Long ownerUid) {
        MoneyBook book = books.save(MoneyBook.create("book", ownerUid));
        memberships.save(MoneyBookUser.owner(book, ownerUid));
        return book;
    }

    private MoneyBookCategory category(MoneyBook book, String name, TransactionType type) {
        return categories.save(MoneyBookCategory.create(book, name, type, 0));
    }

    private MoneyBookAccount account(MoneyBook book, String name) {
        return accounts.save(MoneyBookAccount.create(book, name, AccountType.CASH, 0));
    }

    private MoneyBookUser member(MoneyBook book, Long uid, boolean create, boolean read,
                                 boolean update, boolean delete, boolean accept) {
        MoneyBookUser member = MoneyBookUser.invite(book, uid, false, create, read, update, delete);
        if (accept) member.acceptInvitation();
        return memberships.save(member);
    }

    private CreateRecurringTransactionRequest request(MoneyBookCategory category, MoneyBookAccount account,
                                                      RecurringFrequency frequency, Integer dayOfMonth,
                                                      Integer dayOfWeek, String startDate, String endDate) {
        return new CreateRecurringTransactionRequest(TransactionType.EXPENSE, new BigDecimal("10"),
                category.getCategoryUid(), account.getAccountUid(), frequency, dayOfMonth, dayOfWeek,
                LocalDate.parse(startDate), endDate == null ? null : LocalDate.parse(endDate), null);
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private void error(ErrorCode expected, Runnable action) {
        assertEquals(expected, assertThrows(BusinessException.class, action::run).getErrorCode());
    }

    private record Fixture(Long ownerUid, MoneyBook book, MoneyBookCategory expense, MoneyBookAccount account) {
        Long bookUid() { return book.getMoneyBookUid(); }
        JwtAuthenticationToken auth() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", ownerUid.toString()).build());
        }
    }
}
