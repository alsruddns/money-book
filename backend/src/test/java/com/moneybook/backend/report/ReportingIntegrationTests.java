package com.moneybook.backend.report;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.budget.dto.SaveBudgetRequest;
import com.moneybook.backend.budget.service.BudgetService;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.closing.service.MonthClosingService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.MoneyBookTransfer;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.RecurringTransaction;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.report.service.ReportService;
import com.moneybook.backend.report.dto.ExpenseRankingPeriod;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionRequest;
import com.moneybook.backend.recurring.repository.RecurringTransactionRepository;
import com.moneybook.backend.recurring.service.RecurringTransactionService;
import com.moneybook.backend.transaction.dto.CreateTransactionRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchSort;
import com.moneybook.backend.transaction.dto.UpdateTransactionRequest;
import com.moneybook.backend.transaction.repository.TransactionRepository;
import com.moneybook.backend.transaction.service.TransactionSearchService;
import com.moneybook.backend.transaction.service.TransactionService;
import com.moneybook.backend.transfer.dto.CreateTransferRequest;
import com.moneybook.backend.transfer.dto.UpdateTransferRequest;
import com.moneybook.backend.transfer.repository.TransferRepository;
import com.moneybook.backend.transfer.service.TransferService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reporting_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class ReportingIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryRepository categories;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransferRepository transfers;
    @Autowired private RecurringTransactionRepository recurringRules;
    @Autowired private RecurringTransactionService recurringService;
    @Autowired private TransactionSearchService search;
    @Autowired private TransactionService transactionService;
    @Autowired private TransferService transferService;
    @Autowired private ReportService reports;
    @Autowired private BudgetService budgets;
    @Autowired private MonthClosingService closings;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    @Test
    void searchFiltersPagesSortsEscapesWildcardsAndUsesFixedQueries() {
        Fixture f = fixture();
        transaction(f, TransactionType.EXPENSE, f.food, f.cash, "30", "2026-10-02", "100% lunch");
        transaction(f, TransactionType.EXPENSE, f.food, f.bank, "80", "2026-10-03", "Dinner");
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "500", "2026-10-03", "PAY");
        transaction(f, TransactionType.EXPENSE, f.food, f.cash, "999", "2026-11-01", "later");
        em.flush(); em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var result = search.search(f.uid(), query("2026-10-01", "2026-10-31", TransactionType.EXPENSE,
                f.food.getCategoryUid(), null, null, "20", "100", 0, 1, TransactionSearchSort.AMOUNT_DESC), f.auth());
        assertEquals(2, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(1, result.content().size());
        assertEquals(0, result.content().getFirst().amount().compareTo(new BigDecimal("80")));
        assertTrue(result.first());
        assertFalse(result.last());
        assertTrue(stats.getPrepareStatementCount() <= 4, "search page and count must not load names per row");
        assertEquals(1, search.search(f.uid(), query("2026-10-01", "2026-10-31", null,
                null, null, "100%", null, null, 0, 20, TransactionSearchSort.DATE_DESC), f.auth())
                .totalElements());
        assertEquals(0, search.search(f.uid(), query("2026-10-01", "2026-10-31", null,
                null, null, "100_", null, null, 0, 20, TransactionSearchSort.DATE_DESC), f.auth())
                .totalElements());
        assertEquals(2, search.search(f.uid(), query("2026-10-01", "2026-10-31", TransactionType.EXPENSE,
                null, null, "FOOD", null, null, 0, 20, TransactionSearchSort.DATE_DESC), f.auth())
                .totalElements());
        assertEquals(2, search.search(f.uid(), query("2026-10-01", "2026-10-31", null,
                null, f.bank.getAccountUid(), null, null, null, 0, 20, TransactionSearchSort.DATE_DESC), f.auth())
                .totalElements());
    }

    @Test
    void searchRejectsInvalidRangesAndRequiresReadPermission() {
        Fixture f = fixture();
        assertError(ErrorCode.VALIDATION_FAILED, () -> search.search(f.uid(), query("2026-11-01", "2026-10-01",
                null, null, null, null, null, null, 0, 20, TransactionSearchSort.DATE_DESC), f.auth()));
        assertError(ErrorCode.VALIDATION_FAILED, () -> search.search(f.uid(), query("2026-10-01", "2026-10-02",
                null, null, null, null, null, null, 0, 101, TransactionSearchSort.DATE_DESC), f.auth()));
        assertError(ErrorCode.VALIDATION_FAILED, () -> search.search(f.uid(), query("2026-10-01", "2026-10-02",
                null, null, null, null, "100", "1", 0, 20, TransactionSearchSort.DATE_DESC), f.auth()));
        Long deniedUid = users.save(User.create("denied", null)).getUserUid();
        MoneyBookUser denied = MoneyBookUser.invite(f.book, deniedUid, false, false, false, true, false);
        denied.acceptInvitation(); memberships.save(denied);
        assertError(ErrorCode.MONEY_BOOK_READ_FORBIDDEN, () -> search.search(f.uid(), query("2026-10-01", "2026-10-31",
                null, null, null, null, null, null, 0, 20, TransactionSearchSort.DATE_DESC), auth(deniedUid)));
    }

    @Test
    void yearlyAndPeriodStatisticsUseAggregatesAndExcludeTransfersFromIncome() {
        Fixture f = fixture();
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "100", "2026-09-30", null);
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "200", "2026-10-02", null);
        transaction(f, TransactionType.EXPENSE, f.food, f.cash, "50", "2026-10-02", null);
        transfers.save(MoneyBookTransfer.create(f.book, f.cash, f.bank, new BigDecimal("30"),
                LocalDate.parse("2026-10-02"), null));
        em.flush(); em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var yearly = reports.yearly(f.uid(), 2026, f.auth());
        assertEquals(12, yearly.months().size());
        assertEquals(BigDecimal.ZERO, yearly.months().getFirst().income());
        assertEquals(0, yearly.months().get(9).income().compareTo(new BigDecimal("200")));
        assertEquals(0, yearly.months().get(9).expense().compareTo(new BigDecimal("50")));
        assertEquals(2, yearly.months().get(9).transactionCount());
        assertTrue(stats.getPrepareStatementCount() <= 4, "yearly trend must use one aggregate");
        var categoryRows = reports.categories(f.uid(), LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"), TransactionType.EXPENSE, f.auth());
        assertEquals(1, categoryRows.size());
        assertEquals(new BigDecimal("1.0000"), categoryRows.getFirst().ratio());
        var accountRows = reports.accounts(f.uid(), LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"), f.auth());
        var cash = accountRows.stream().filter(row -> row.accountUid().equals(f.cash.getAccountUid())).findFirst().orElseThrow();
        assertEquals(0, cash.netChange().compareTo(new BigDecimal("-80")));
        var bank = accountRows.stream().filter(row -> row.accountUid().equals(f.bank.getAccountUid())).findFirst().orElseThrow();
        assertEquals(0, bank.netChange().compareTo(new BigDecimal("230")));
        assertError(ErrorCode.VALIDATION_FAILED, () -> reports.categories(f.uid(),
                LocalDate.parse("2026-12-01"), LocalDate.parse("2026-10-01"), TransactionType.EXPENSE, f.auth()));
    }

    @Test
    void expenseRankingIsLimitedToTwentyAndUsesDeterministicExpenseOnlyOrdering() {
        Fixture f = fixture();
        for (int amount = 1; amount <= 25; amount++) {
            transaction(f, TransactionType.EXPENSE, f.food, f.cash, Integer.toString(amount),
                    amount < 4 ? "2026-10-02" : "2026-10-03", "expense");
        }
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "9999", "2026-10-03", "income");
        transaction(f, TransactionType.EXPENSE, f.food, f.cash, "5000", "2026-11-01", "other month");

        var rows = reports.expenseRanking(f.uid(), ExpenseRankingPeriod.MONTH, 2026, 10, f.auth());
        assertEquals(20, rows.size());
        assertEquals(1, rows.getFirst().rank());
        assertEquals(0, rows.getFirst().amount().compareTo(new BigDecimal("25")));
        assertEquals(6, rows.getLast().amount().intValue());
        assertTrue(rows.stream().allMatch(row -> row.transactionDate().getMonthValue() == 10));

        var yearly = reports.expenseRanking(f.uid(), ExpenseRankingPeriod.YEAR, 2026, null, f.auth());
        assertEquals(20, yearly.size());
        assertEquals(0, yearly.getFirst().amount().compareTo(new BigDecimal("5000")));
        assertTrue(reports.expenseRanking(f.uid(), ExpenseRankingPeriod.YEAR, 2025, null, f.auth()).isEmpty());
        assertError(ErrorCode.VALIDATION_FAILED, () -> reports.expenseRanking(
                f.uid(), ExpenseRankingPeriod.MONTH, 2026, null, f.auth()));
        assertError(ErrorCode.VALIDATION_FAILED, () -> reports.expenseRanking(
                f.uid(), ExpenseRankingPeriod.YEAR, 2026, 10, f.auth()));
    }

    @Test
    void monthlyReportComparesPreviousMonthAndBudgetWithoutTransfers() {
        Fixture f = fixture();
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "100", "2026-09-30", null);
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "150", "2026-10-02", null);
        transaction(f, TransactionType.EXPENSE, f.food, f.cash, "60", "2026-10-02", null);
        budgets.save(f.uid(), 2026, 10, new SaveBudgetRequest(new BigDecimal("50"), List.of()), f.auth());
        var report = reports.monthly(f.uid(), 2026, 10, f.auth());
        assertEquals(0, report.incomeChange().compareTo(new BigDecimal("50")));
        assertEquals(new BigDecimal("50.00"), report.incomeChangeRate());
        assertNull(report.expenseChangeRate());
        assertEquals(new BigDecimal("120.00"), report.budgetUsageRate());
        assertEquals(0, report.remainingBudget().compareTo(new BigDecimal("-10")));
        assertTrue(report.overBudget());
        assertEquals(2, report.transactionCount());
    }

    @Test
    void closeSnapshotsTotalsBlocksWritesAndCancelReopensMonth() {
        Fixture f = fixture();
        transaction(f, TransactionType.EXPENSE, f.food, f.cash, "40", "2026-09-02", null);
        var closing = closings.close(f.uid(), 2026, 9, f.auth());
        assertEquals(0, closing.expense().compareTo(new BigDecimal("40")));
        assertEquals(closing.closingUid(), closings.get(f.uid(), 2026, 9, f.auth()).closingUid());
        assertError(ErrorCode.MONTH_ALREADY_CLOSED, () -> closings.close(f.uid(), 2026, 9, f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> transactionService.create(f.uid(),
                new CreateTransactionRequest(TransactionType.EXPENSE, new BigDecimal("1"), LocalDate.parse("2026-09-03"),
                        f.food.getCategoryUid(), f.cash.getAccountUid(), null), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> transferService.create(f.uid(),
                new CreateTransferRequest(f.cash.getAccountUid(), f.bank.getAccountUid(), new BigDecimal("1"),
                        LocalDate.parse("2026-09-03"), null), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> budgets.save(f.uid(), 2026, 9,
                new SaveBudgetRequest(new BigDecimal("100"), List.of()), f.auth()));
        closings.cancel(f.uid(), 2026, 9, f.auth());
        assertError(ErrorCode.MONTH_NOT_CLOSED, () -> closings.get(f.uid(), 2026, 9, f.auth()));
        assertEquals(0, transactionService.create(f.uid(),
                new CreateTransactionRequest(TransactionType.EXPENSE, new BigDecimal("1"), LocalDate.parse("2026-09-03"),
                        f.food.getCategoryUid(), f.cash.getAccountUid(), null), f.auth()).amount()
                .compareTo(new BigDecimal("1")));
    }

    @Test
    void closeRequiresUpdateWhileReaderMayReadSnapshot() {
        Fixture f = fixture();
        closings.close(f.uid(), 2026, 9, f.auth());
        Long readerUid = users.save(User.create("reader", null)).getUserUid();
        MoneyBookUser reader = MoneyBookUser.invite(f.book, readerUid, false, false, true, false, false);
        reader.acceptInvitation(); memberships.save(reader);
        assertEquals(2026, closings.get(f.uid(), 2026, 9, auth(readerUid)).year());
        assertError(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN,
                () -> closings.cancel(f.uid(), 2026, 9, auth(readerUid)));
    }

    @Test
    void closedMonthlyReportKeepsPreviousComparisonCapturedAtClosing() {
        Fixture f = fixture();
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "100", "2026-08-02", null);
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "200", "2026-09-02", null);
        closings.close(f.uid(), 2026, 9, f.auth());
        transaction(f, TransactionType.INCOME, f.salary, f.bank, "50", "2026-08-03", null);
        var report = reports.monthly(f.uid(), 2026, 9, f.auth());
        assertEquals(0, report.previousIncome().compareTo(new BigDecimal("100")));
        assertEquals(0, report.incomeChange().compareTo(new BigDecimal("100")));
        assertEquals(new BigDecimal("100.00"), report.incomeChangeRate());
    }

    @Test
    void closedMonthRejectsEditsDeletesDateMovesAndRecurringGeneration() {
        Fixture f = fixture();
        var closedTransaction = transactions.save(MoneyBookTransaction.create(f.book, TransactionType.EXPENSE,
                new BigDecimal("5"), LocalDate.parse("2026-09-02"), f.food, f.cash, null));
        var openTransaction = transactions.save(MoneyBookTransaction.create(f.book, TransactionType.EXPENSE,
                new BigDecimal("7"), LocalDate.parse("2026-10-02"), f.food, f.cash, null));
        var closedTransfer = transfers.save(MoneyBookTransfer.create(f.book, f.cash, f.bank,
                new BigDecimal("3"), LocalDate.parse("2026-09-02"), null));
        var openTransfer = transfers.save(MoneyBookTransfer.create(f.book, f.cash, f.bank,
                new BigDecimal("4"), LocalDate.parse("2026-10-02"), null));
        recurringRules.save(RecurringTransaction.create(f.book, TransactionType.EXPENSE, new BigDecimal("2"),
                f.food, f.cash, RecurringFrequency.MONTHLY, 5, null, LocalDate.parse("2026-09-01"), null, null));
        closings.close(f.uid(), 2026, 9, f.auth());
        assertError(ErrorCode.MONTH_CLOSED, () -> transactionService.update(f.uid(), closedTransaction.getTransactionUid(),
                new UpdateTransactionRequest(TransactionType.EXPENSE, new BigDecimal("6"),
                        LocalDate.parse("2026-10-02"), f.food.getCategoryUid(), f.cash.getAccountUid(), null), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> transactionService.update(f.uid(), openTransaction.getTransactionUid(),
                new UpdateTransactionRequest(TransactionType.EXPENSE, new BigDecimal("6"),
                        LocalDate.parse("2026-09-03"), f.food.getCategoryUid(), f.cash.getAccountUid(), null), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED,
                () -> transactionService.delete(f.uid(), closedTransaction.getTransactionUid(), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> transferService.update(f.uid(), closedTransfer.getTransferUid(),
                new UpdateTransferRequest(f.cash.getAccountUid(), f.bank.getAccountUid(), new BigDecimal("6"),
                        LocalDate.parse("2026-10-02"), null), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> transferService.update(f.uid(), openTransfer.getTransferUid(),
                new UpdateTransferRequest(f.cash.getAccountUid(), f.bank.getAccountUid(), new BigDecimal("6"),
                        LocalDate.parse("2026-09-03"), null), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED,
                () -> transferService.delete(f.uid(), closedTransfer.getTransferUid(), f.auth()));
        assertError(ErrorCode.MONTH_CLOSED, () -> recurringService.generate(f.uid(),
                new GenerateRecurringTransactionRequest(LocalDate.parse("2026-10-10")), f.auth()));
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

    private void transaction(Fixture f, TransactionType type, MoneyBookCategory category, MoneyBookAccount account,
                             String amount, String date, String memo) {
        transactions.save(MoneyBookTransaction.create(f.book, type, new BigDecimal(amount), LocalDate.parse(date),
                category, account, memo));
    }

    private TransactionSearchRequest query(String start, String end, TransactionType type, Long categoryUid,
                                           Long accountUid, String keyword, String min, String max,
                                           int page, int size, TransactionSearchSort sort) {
        return new TransactionSearchRequest(LocalDate.parse(start), LocalDate.parse(end), type, categoryUid, accountUid,
                keyword, min == null ? null : new BigDecimal(min), max == null ? null : new BigDecimal(max),
                page, size, sort);
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private void assertError(ErrorCode code, Runnable action) {
        assertEquals(code, assertThrows(BusinessException.class, action::run).getErrorCode());
    }

    private record Fixture(Long owner, MoneyBook book, MoneyBookCategory salary, MoneyBookCategory food,
                           MoneyBookAccount cash, MoneyBookAccount bank) {
        Long uid() { return book.getMoneyBookUid(); }
        JwtAuthenticationToken auth() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", owner.toString()).build());
        }
    }
}
