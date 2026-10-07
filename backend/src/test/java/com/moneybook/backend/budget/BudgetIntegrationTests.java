package com.moneybook.backend.budget;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.budget.dto.CategoryBudgetRequest;
import com.moneybook.backend.budget.dto.SaveBudgetRequest;
import com.moneybook.backend.budget.service.BudgetService;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:budget_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class BudgetIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryRepository categories;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransferRepository transfers;
    @Autowired private BudgetService budgets;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    @Test
    void createUpdateAndAggregateExpenseWithoutIncomeOrTransfers() {
        Fixture f = fixture();
        MoneyBookCategory travel = categories.save(MoneyBookCategory.create(f.book, "travel", TransactionType.EXPENSE, 1));
        transaction(f, f.expense, TransactionType.EXPENSE, "300", "2026-10-01");
        transaction(f, travel, TransactionType.EXPENSE, "200", "2026-10-02");
        transaction(f, f.income, TransactionType.INCOME, "999", "2026-10-02");
        transaction(f, f.expense, TransactionType.EXPENSE, "888", "2026-11-01");
        transfers.save(MoneyBookTransfer.create(f.book, f.cash, f.bank, new BigDecimal("777"),
                LocalDate.parse("2026-10-03"), null));
        var empty = budgets.get(f.book.getMoneyBookUid(), 2026, 10, f.auth());
        assertFalse(empty.configured());
        assertNull(empty.totalBudget());
        assertEquals(0, empty.totalExpense().compareTo(new BigDecimal("500")));

        var created = budgets.save(f.book.getMoneyBookUid(), 2026, 10,
                request("1000", item(f.expense, "400"), item(travel, "300")), f.auth());
        assertTrue(created.configured());
        assertNotNull(created.budgetUid());
        assertEquals(0, created.totalExpense().compareTo(new BigDecimal("500")));
        assertEquals(0, created.remainingBudget().compareTo(new BigDecimal("500")));
        assertEquals(new BigDecimal("50.00"), created.usageRate());
        assertFalse(created.overBudget());
        assertEquals(new BigDecimal("75.00"), created.categories().getFirst().usageRate());
        assertEquals(0, created.categories().getFirst().expenseAmount().compareTo(new BigDecimal("300")));

        var updated = budgets.save(f.book.getMoneyBookUid(), 2026, 10,
                request("400", item(f.expense, "200")), f.auth());
        assertEquals(created.budgetUid(), updated.budgetUid());
        assertEquals(1, updated.categories().size());
        assertEquals(0, updated.remainingBudget().compareTo(new BigDecimal("-100")));
        assertEquals(new BigDecimal("125.00"), updated.usageRate());
        assertTrue(updated.overBudget());
        assertTrue(updated.categories().getFirst().overBudget());
        em.flush();
        em.clear();
        assertEquals(1, budgets.get(f.book.getMoneyBookUid(), 2026, 10, f.auth()).categories().size());
    }

    @Test
    void validatesCategoryOwnershipTypeDuplicatesSumAndAmounts() {
        Fixture f = fixture();
        Fixture other = fixture();
        Long uid = f.book.getMoneyBookUid();
        assertError(ErrorCode.VALIDATION_FAILED,
                () -> budgets.save(uid, 2026, 13, request("10"), f.auth()));
        assertError(ErrorCode.VALIDATION_FAILED,
                () -> budgets.save(uid, 2026, 10, request("-1"), f.auth()));
        assertError(ErrorCode.VALIDATION_FAILED,
                () -> budgets.save(uid, 2026, 10, request("10", item(f.expense, "-1")), f.auth()));
        assertError(ErrorCode.VALIDATION_FAILED,
                () -> budgets.save(uid, 2026, 10, request("10", item(f.expense, "1"), item(f.expense, "2")), f.auth()));
        assertError(ErrorCode.BUDGET_CATEGORY_SUM_EXCEEDED,
                () -> budgets.save(uid, 2026, 10, request("10", item(f.expense, "11")), f.auth()));
        assertError(ErrorCode.BUDGET_CATEGORY_NOT_EXPENSE,
                () -> budgets.save(uid, 2026, 10, request("10", item(f.income, "1")), f.auth()));
        assertError(ErrorCode.CATEGORY_NOT_IN_MONEY_BOOK,
                () -> budgets.save(uid, 2026, 10, request("10", item(other.expense, "1")), f.auth()));
        var noTotal = budgets.save(uid, 2026, 10, request(null, item(f.expense, "100")), f.auth());
        assertNull(noTotal.totalBudget());
        assertNull(noTotal.usageRate());
        var zero = budgets.save(uid, 2026, 10, request("0", item(f.expense, "0")), f.auth());
        assertEquals(BigDecimal.ZERO, zero.usageRate());
    }

    @Test
    void readAndUpdatePermissionsAreEnforced() {
        Fixture f = fixture();
        Long readerUid = users.save(User.create("reader", null)).getUserUid();
        MoneyBookUser reader = MoneyBookUser.invite(f.book, readerUid, false, false, true, false, false);
        reader.acceptInvitation();
        memberships.save(reader);
        assertFalse(budgets.get(f.book.getMoneyBookUid(), 2026, 10, auth(readerUid)).configured());
        assertError(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN,
                () -> budgets.save(f.book.getMoneyBookUid(), 2026, 10, request("100"), auth(readerUid)));
        Long noReadUid = users.save(User.create("no-read", null)).getUserUid();
        MoneyBookUser noRead = MoneyBookUser.invite(f.book, noReadUid, false, false, false, true, false);
        noRead.acceptInvitation();
        memberships.save(noRead);
        assertError(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> budgets.get(f.book.getMoneyBookUid(), 2026, 10, auth(noReadUid)));
    }

    @Test
    void categoryAggregationUsesFixedQueryCount() {
        Fixture f = fixture();
        for (int i = 0; i < 12; i++) {
            MoneyBookCategory category = categories.save(MoneyBookCategory.create(f.book, "expense" + i,
                    TransactionType.EXPENSE, i + 1));
            transaction(f, category, TransactionType.EXPENSE, "1", "2026-10-01");
        }
        budgets.save(f.book.getMoneyBookUid(), 2026, 10, request(null), f.auth());
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var response = budgets.get(f.book.getMoneyBookUid(), 2026, 10, f.auth());
        assertEquals(0, response.totalExpense().compareTo(new BigDecimal("12")));
        assertTrue(stats.getPrepareStatementCount() <= 5, "budget read query count must be fixed");
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

    private CategoryBudgetRequest item(MoneyBookCategory category, String amount) {
        return new CategoryBudgetRequest(category.getCategoryUid(), new BigDecimal(amount));
    }

    private SaveBudgetRequest request(String total, CategoryBudgetRequest... items) {
        return new SaveBudgetRequest(total == null ? null : new BigDecimal(total), List.of(items));
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private void assertError(ErrorCode code, Runnable action) {
        assertEquals(code, assertThrows(BusinessException.class, action::run).getErrorCode());
    }

    private record Fixture(Long owner, MoneyBook book, MoneyBookCategory income, MoneyBookCategory expense,
                           MoneyBookAccount cash, MoneyBookAccount bank) {
        JwtAuthenticationToken auth() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", owner.toString()).build());
        }
    }
}
