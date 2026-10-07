package com.moneybook.backend.transaction.service;

import com.moneybook.backend.account.dto.CreateAccountRequest;
import com.moneybook.backend.account.dto.UpdateAccountRequest;
import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.account.service.AccountService;
import com.moneybook.backend.category.dto.CreateCategoryRequest;
import com.moneybook.backend.category.dto.UpdateCategoryRequest;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.category.service.CategoryService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.transaction.dto.CreateTransactionRequest;
import com.moneybook.backend.transaction.dto.TransactionResponse;
import com.moneybook.backend.transaction.dto.UpdateTransactionRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ledger_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class LedgerIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryRepository categories;
    @Autowired private AccountRepository accounts;
    @Autowired private CategoryService categoryService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void categoryCrudFiltersSortsAndRejectsDuplicateOrForeignCategory() {
        Fixture f = fixture();
        var expense = categoryService.create(f.bookUid(), new CreateCategoryRequest("food", TransactionType.EXPENSE, 2), f.owner());
        var income = categoryService.create(f.bookUid(), new CreateCategoryRequest("salary", TransactionType.INCOME, 0), f.owner());
        var earlier = categoryService.create(f.bookUid(), new CreateCategoryRequest("travel", TransactionType.EXPENSE, 1), f.owner());
        assertEquals(List.of(earlier.categoryUid(), expense.categoryUid(), income.categoryUid()),
                categoryService.list(f.bookUid(), null, f.owner()).stream().map(c -> c.categoryUid()).toList());
        assertEquals(List.of(earlier.categoryUid(), expense.categoryUid()),
                categoryService.list(f.bookUid(), TransactionType.EXPENSE, f.owner()).stream()
                        .map(c -> c.categoryUid()).toList());
        error(ErrorCode.CATEGORY_ALREADY_EXISTS, () -> categoryService.create(f.bookUid(),
                new CreateCategoryRequest("food", TransactionType.EXPENSE, 0), f.owner()));
        var updated = categoryService.update(f.bookUid(), expense.categoryUid(),
                new UpdateCategoryRequest("groceries", 3), f.owner());
        assertEquals("groceries", updated.name());
        assertEquals(TransactionType.EXPENSE, updated.transactionType());
        error(ErrorCode.CATEGORY_ALREADY_EXISTS, () -> categoryService.update(f.bookUid(),
                expense.categoryUid(), new UpdateCategoryRequest("travel", 0), f.owner()));
        MoneyBook other = book(f.ownerUid());
        error(ErrorCode.CATEGORY_NOT_FOUND, () -> categoryService.update(other.getMoneyBookUid(),
                expense.categoryUid(), new UpdateCategoryRequest("wrong", 0), f.owner()));
        categoryService.delete(f.bookUid(), expense.categoryUid(), f.owner());
        assertTrue(categories.findById(expense.categoryUid()).isEmpty());
    }

    @Test
    void accountCrudSortsAndRejectsDuplicateOrForeignAccount() {
        Fixture f = fixture();
        var card = accountService.create(f.bookUid(), new CreateAccountRequest("card", AccountType.CARD, 2), f.owner());
        var cash = accountService.create(f.bookUid(), new CreateAccountRequest("cash", AccountType.CASH, 0), f.owner());
        assertEquals(List.of(cash.accountUid(), card.accountUid()),
                accountService.list(f.bookUid(), f.owner()).stream().map(a -> a.accountUid()).toList());
        error(ErrorCode.ACCOUNT_ALREADY_EXISTS, () -> accountService.create(f.bookUid(),
                new CreateAccountRequest("card", AccountType.BANK, 0), f.owner()));
        var updated = accountService.update(f.bookUid(), card.accountUid(),
                new UpdateAccountRequest("bank", AccountType.BANK, 1), f.owner());
        assertEquals("bank", updated.name());
        assertEquals(AccountType.BANK, updated.accountType());
        error(ErrorCode.ACCOUNT_ALREADY_EXISTS, () -> accountService.update(f.bookUid(), card.accountUid(),
                new UpdateAccountRequest("cash", AccountType.ETC, 0), f.owner()));
        MoneyBook other = book(f.ownerUid());
        error(ErrorCode.ACCOUNT_NOT_FOUND, () -> accountService.update(other.getMoneyBookUid(),
                card.accountUid(), new UpdateAccountRequest("wrong", AccountType.ETC, 0), f.owner()));
        accountService.delete(f.bookUid(), card.accountUid(), f.owner());
        assertTrue(accounts.findById(card.accountUid()).isEmpty());
    }

    @Test
    void transactionCrudChecksReferencesTypesAndInUseDeletion() {
        Fixture f = fixture();
        MoneyBookCategory expense = category(f.book(), "food", TransactionType.EXPENSE);
        MoneyBookCategory income = category(f.book(), "salary", TransactionType.INCOME);
        MoneyBookAccount cash = account(f.book(), "cash");
        TransactionResponse spending = transactionService.create(f.bookUid(), request(
                TransactionType.EXPENSE, "15000", "2026-10-02", expense, cash), f.owner());
        TransactionResponse earning = transactionService.create(f.bookUid(), request(
                TransactionType.INCOME, "25000", "2026-10-03", income, cash), f.owner());
        assertEquals("food", spending.categoryName());
        assertEquals("cash", spending.accountName());
        assertEquals(TransactionType.INCOME, earning.transactionType());
        assertEquals(spending, transactionService.detail(f.bookUid(), spending.transactionUid(), f.owner()));
        error(ErrorCode.CATEGORY_IN_USE, () -> categoryService.delete(f.bookUid(), expense.getCategoryUid(), f.owner()));
        error(ErrorCode.ACCOUNT_IN_USE, () -> accountService.delete(f.bookUid(), cash.getAccountUid(), f.owner()));

        var modified = transactionService.update(f.bookUid(), spending.transactionUid(),
                new UpdateTransactionRequest(TransactionType.INCOME, new BigDecimal("500"),
                        LocalDate.parse("2026-10-04"), income.getCategoryUid(), cash.getAccountUid(), "bonus"), f.owner());
        assertEquals("salary", modified.categoryName());
        assertEquals("bonus", modified.memo());
        transactionService.delete(f.bookUid(), spending.transactionUid(), f.owner());
        error(ErrorCode.TRANSACTION_NOT_FOUND,
                () -> transactionService.detail(f.bookUid(), spending.transactionUid(), f.owner()));
    }

    @Test
    void transactionRejectsZeroNegativeForeignReferencesAndCategoryTypeMismatch() {
        Fixture f = fixture();
        MoneyBookCategory expense = category(f.book(), "food", TransactionType.EXPENSE);
        MoneyBookCategory income = category(f.book(), "salary", TransactionType.INCOME);
        MoneyBookAccount cash = account(f.book(), "cash");
        MoneyBook other = book(f.ownerUid());
        MoneyBookCategory foreignCategory = category(other, "other", TransactionType.EXPENSE);
        MoneyBookAccount foreignAccount = account(other, "other");
        for (String amount : List.of("0", "-1")) {
            error(ErrorCode.VALIDATION_FAILED, () -> transactionService.create(f.bookUid(),
                    request(TransactionType.EXPENSE, amount, "2026-10-02", expense, cash), f.owner()));
        }
        error(ErrorCode.CATEGORY_NOT_IN_MONEY_BOOK, () -> transactionService.create(f.bookUid(),
                request(TransactionType.EXPENSE, "1", "2026-10-02", foreignCategory, cash), f.owner()));
        error(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK, () -> transactionService.create(f.bookUid(),
                request(TransactionType.EXPENSE, "1", "2026-10-02", expense, foreignAccount), f.owner()));
        error(ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH, () -> transactionService.create(f.bookUid(),
                request(TransactionType.EXPENSE, "1", "2026-10-02", income, cash), f.owner()));
        TransactionResponse valid = transactionService.create(f.bookUid(),
                request(TransactionType.EXPENSE, "1", "2026-10-02", expense, cash), f.owner());
        error(ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH, () -> transactionService.update(f.bookUid(),
                valid.transactionUid(), new UpdateTransactionRequest(TransactionType.INCOME, new BigDecimal("2"),
                        LocalDate.parse("2026-10-02"), expense.getCategoryUid(), cash.getAccountUid(), null), f.owner()));
        error(ErrorCode.VALIDATION_FAILED, () -> transactionService.update(f.bookUid(),
                valid.transactionUid(), new UpdateTransactionRequest(TransactionType.EXPENSE, BigDecimal.ZERO,
                        LocalDate.parse("2026-10-02"), expense.getCategoryUid(), cash.getAccountUid(), null), f.owner()));
        error(ErrorCode.CATEGORY_NOT_IN_MONEY_BOOK, () -> transactionService.update(f.bookUid(),
                valid.transactionUid(), new UpdateTransactionRequest(TransactionType.EXPENSE, new BigDecimal("2"),
                        LocalDate.parse("2026-10-02"), foreignCategory.getCategoryUid(), cash.getAccountUid(), null), f.owner()));
        error(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK, () -> transactionService.update(f.bookUid(),
                valid.transactionUid(), new UpdateTransactionRequest(TransactionType.EXPENSE, new BigDecimal("2"),
                        LocalDate.parse("2026-10-02"), expense.getCategoryUid(), foreignAccount.getAccountUid(), null), f.owner()));
        error(ErrorCode.TRANSACTION_NOT_FOUND,
                () -> transactionService.detail(other.getMoneyBookUid(), valid.transactionUid(), f.owner()));
        error(ErrorCode.TRANSACTION_NOT_FOUND,
                () -> transactionService.delete(other.getMoneyBookUid(), valid.transactionUid(), f.owner()));
    }

    @Test
    void monthRangeOrderNamesAndEmptyResultUseOneJoinedQuery() {
        Fixture f = fixture();
        MoneyBookCategory expense = category(f.book(), "food", TransactionType.EXPENSE);
        MoneyBookAccount cash = account(f.book(), "cash");
        transactionService.create(f.bookUid(), request(TransactionType.EXPENSE, "1", "2026-09-30", expense, cash), f.owner());
        var first = transactionService.create(f.bookUid(), request(TransactionType.EXPENSE, "2", "2026-10-02", expense, cash), f.owner());
        var second = transactionService.create(f.bookUid(), request(TransactionType.EXPENSE, "3", "2026-10-02", expense, cash), f.owner());
        var newest = transactionService.create(f.bookUid(), request(TransactionType.EXPENSE, "4", "2026-10-31", expense, cash), f.owner());
        transactionService.create(f.bookUid(), request(TransactionType.EXPENSE, "5", "2026-11-01", expense, cash), f.owner());
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<TransactionResponse> result = transactionService.list(f.bookUid(), 2026, 10, f.owner());
        assertEquals(List.of(newest.transactionUid(), second.transactionUid(), first.transactionUid()),
                result.stream().map(TransactionResponse::transactionUid).toList());
        assertTrue(result.stream().allMatch(row -> row.categoryName().equals("food") && row.accountName().equals("cash")));
        assertTrue(statistics.getPrepareStatementCount() <= 3, "monthly rows must not cause N+1 queries");
        assertTrue(transactionService.list(f.bookUid(), 2026, 12, f.owner()).isEmpty());
    }

    @Test
    void independentCrudPermissionsRequireAcceptedMembershipAndOwnerAlwaysWorks() {
        Fixture f = fixture();
        Long readerUid = user("reader");
        Long creatorUid = user("creator");
        Long updaterUid = user("updater");
        Long deleterUid = user("deleter");
        Long pendingUid = user("pending");
        Long rejectedUid = user("rejected");
        member(f.book(), readerUid, false, false, true, false, false, true);
        member(f.book(), creatorUid, false, true, false, false, false, true);
        member(f.book(), updaterUid, false, false, false, true, false, true);
        member(f.book(), deleterUid, false, false, false, false, true, true);
        member(f.book(), pendingUid, true, true, true, true, true, false);
        MoneyBookUser rejected = member(f.book(), rejectedUid, true, true, true, true, true, false);
        rejected.rejectInvitation();

        MoneyBookCategory expense = category(f.book(), "food", TransactionType.EXPENSE);
        MoneyBookAccount cash = account(f.book(), "cash");
        var entry = transactionService.create(f.bookUid(),
                request(TransactionType.EXPENSE, "10", "2026-10-02", expense, cash), f.owner());
        assertEquals(1, categoryService.list(f.bookUid(), null, auth(readerUid)).size());
        assertEquals(1, accountService.list(f.bookUid(), auth(readerUid)).size());
        assertEquals(entry.transactionUid(), transactionService.detail(f.bookUid(), entry.transactionUid(), auth(readerUid)).transactionUid());
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN, () -> categoryService.create(f.bookUid(),
                new CreateCategoryRequest("no", TransactionType.EXPENSE, 0), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN, () -> accountService.create(f.bookUid(),
                new CreateAccountRequest("no", AccountType.CASH, 0), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN, () -> transactionService.create(f.bookUid(),
                request(TransactionType.EXPENSE, "2", "2026-10-02", expense, cash), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN, () -> transactionService.list(f.bookUid(), 2026, 10, auth(creatorUid)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> transactionService.detail(f.bookUid(), entry.transactionUid(), auth(creatorUid)));
        var created = accountService.create(f.bookUid(),
                new CreateAccountRequest("creator-account", AccountType.BANK, 0), auth(creatorUid));
        assertEquals("creator-account", created.name());
        var updated = categoryService.update(f.bookUid(), expense.getCategoryUid(),
                new UpdateCategoryRequest("groceries", 1), auth(updaterUid));
        assertEquals("groceries", updated.name());
        error(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN, () -> accountService.update(f.bookUid(), cash.getAccountUid(),
                new UpdateAccountRequest("no", AccountType.CASH, 0), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN, () -> transactionService.update(f.bookUid(), entry.transactionUid(),
                new UpdateTransactionRequest(TransactionType.EXPENSE, BigDecimal.ONE,
                        LocalDate.parse("2026-10-02"), expense.getCategoryUid(), cash.getAccountUid(), null), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_DELETE_FORBIDDEN,
                () -> transactionService.delete(f.bookUid(), entry.transactionUid(), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_DELETE_FORBIDDEN,
                () -> categoryService.delete(f.bookUid(), expense.getCategoryUid(), auth(readerUid)));
        error(ErrorCode.MONEY_BOOK_DELETE_FORBIDDEN,
                () -> accountService.delete(f.bookUid(), cash.getAccountUid(), auth(readerUid)));
        transactionService.delete(f.bookUid(), entry.transactionUid(), auth(deleterUid));
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN, () -> accountService.create(f.bookUid(),
                new CreateAccountRequest("pending", AccountType.CASH, 0), auth(pendingUid)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN, () -> categoryService.list(f.bookUid(), null, auth(rejectedUid)));
        assertEquals(2, accountService.list(f.bookUid(), f.owner()).size());
    }

    @Test
    void acceptedAdminHasAllCrudRightsWithoutAdminBypass() {
        Fixture f = fixture();
        Long adminUid = user("admin");
        MoneyBookUser admin = member(f.book(), adminUid, true, false, false, false, false, true);
        assertTrue(admin.isCanCreate() && admin.isCanRead() && admin.isCanUpdate() && admin.isCanDelete());
        var created = categoryService.create(f.bookUid(),
                new CreateCategoryRequest("admin", TransactionType.EXPENSE, 0), auth(adminUid));
        assertEquals(1, categoryService.list(f.bookUid(), null, auth(adminUid)).size());
        categoryService.update(f.bookUid(), created.categoryUid(),
                new UpdateCategoryRequest("renamed", 0), auth(adminUid));
        categoryService.delete(f.bookUid(), created.categoryUid(), auth(adminUid));
    }

    @Test
    void emptyCategoryAndAccountListsAreSuccessful() {
        Fixture f = fixture();
        assertTrue(categoryService.list(f.bookUid(), null, f.owner()).isEmpty());
        assertTrue(accountService.list(f.bookUid(), f.owner()).isEmpty());
    }

    @Test
    void unrelatedAndInactiveUsersCannotAccessLedger() {
        Fixture f = fixture();
        Long unrelatedUid = user("unrelated");
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> categoryService.list(f.bookUid(), null, auth(unrelatedUid)));
        users.findById(unrelatedUid).orElseThrow().changeStatus(UserStatus.INACTIVE);
        error(ErrorCode.USER_INACTIVE,
                () -> accountService.list(f.bookUid(), auth(unrelatedUid)));
    }

    private Fixture fixture() {
        Long ownerUid = user("owner");
        MoneyBook book = book(ownerUid);
        return new Fixture(ownerUid, book);
    }

    private Long user(String nickname) { return users.save(User.create(nickname, null)).getUserUid(); }

    private MoneyBook book(Long ownerUid) {
        MoneyBook book = books.save(MoneyBook.create("shared", ownerUid));
        memberships.save(MoneyBookUser.owner(book, ownerUid));
        return book;
    }

    private MoneyBookUser member(MoneyBook book, Long uid, boolean admin, boolean create,
                                 boolean read, boolean update, boolean delete, boolean accept) {
        MoneyBookUser member = MoneyBookUser.invite(book, uid, admin, create, read, update, delete);
        if (accept) member.acceptInvitation();
        return memberships.save(member);
    }

    private MoneyBookCategory category(MoneyBook book, String name, TransactionType type) {
        return categories.save(MoneyBookCategory.create(book, name, type, 0));
    }

    private MoneyBookAccount account(MoneyBook book, String name) {
        return accounts.save(MoneyBookAccount.create(book, name, AccountType.CASH, 0));
    }

    private CreateTransactionRequest request(TransactionType type, String amount, String date,
                                             MoneyBookCategory category, MoneyBookAccount account) {
        return new CreateTransactionRequest(type, new BigDecimal(amount), LocalDate.parse(date),
                category.getCategoryUid(), account.getAccountUid(), null);
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private void error(ErrorCode expected, Runnable operation) {
        assertEquals(expected, assertThrows(BusinessException.class, operation::run).getErrorCode());
    }

    private record Fixture(Long ownerUid, MoneyBook book) {
        Long bookUid() { return book.getMoneyBookUid(); }
        JwtAuthenticationToken owner() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", ownerUid.toString()).build());
        }
    }
}
