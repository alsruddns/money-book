package com.moneybook.backend.transfer;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.transfer.dto.CreateTransferRequest;
import com.moneybook.backend.transfer.dto.UpdateTransferRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:transfer_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class TransferIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private AccountRepository accounts;
    @Autowired private TransferService service;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void createDetailUpdateDeleteAndCrossBookProtection() {
        Fixture f = fixture();
        MoneyBookAccount from = account(f.book(), "cash");
        MoneyBookAccount to = account(f.book(), "bank");
        MoneyBook otherBook = book(f.ownerUid());
        MoneyBookAccount foreign = account(otherBook, "foreign");
        var created = service.create(f.bookUid(), request(from, to, "50000", "2026-10-02"), f.auth());
        assertEquals("cash", created.fromAccountName());
        assertEquals("bank", created.toAccountName());
        assertEquals(created, service.detail(f.bookUid(), created.transferUid(), f.auth()));
        error(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK,
                () -> service.create(f.bookUid(), request(foreign, to, "1", "2026-10-02"), f.auth()));
        error(ErrorCode.ACCOUNT_NOT_IN_MONEY_BOOK,
                () -> service.create(f.bookUid(), request(from, foreign, "1", "2026-10-02"), f.auth()));
        var updated = service.update(f.bookUid(), created.transferUid(),
                new UpdateTransferRequest(to.getAccountUid(), from.getAccountUid(), new BigDecimal("10000"),
                        LocalDate.parse("2026-10-03"), "reverse"), f.auth());
        assertEquals("bank", updated.fromAccountName());
        assertEquals("reverse", updated.memo());
        error(ErrorCode.TRANSFER_NOT_FOUND,
                () -> service.detail(otherBook.getMoneyBookUid(), created.transferUid(), f.auth()));
        service.delete(f.bookUid(), created.transferUid(), f.auth());
        error(ErrorCode.TRANSFER_NOT_FOUND,
                () -> service.detail(f.bookUid(), created.transferUid(), f.auth()));
    }

    @Test
    void invalidAccountsAndAmountsAreRejectedOnCreateAndUpdate() {
        Fixture f = fixture();
        MoneyBookAccount from = account(f.book(), "cash");
        MoneyBookAccount to = account(f.book(), "bank");
        error(ErrorCode.SAME_TRANSFER_ACCOUNT,
                () -> service.create(f.bookUid(), request(from, from, "1", "2026-10-02"), f.auth()));
        for (String amount : List.of("0", "-1")) {
            error(ErrorCode.VALIDATION_FAILED,
                    () -> service.create(f.bookUid(), request(from, to, amount, "2026-10-02"), f.auth()));
        }
        var created = service.create(f.bookUid(), request(from, to, "1", "2026-10-02"), f.auth());
        error(ErrorCode.SAME_TRANSFER_ACCOUNT, () -> service.update(f.bookUid(), created.transferUid(),
                new UpdateTransferRequest(from.getAccountUid(), from.getAccountUid(), BigDecimal.ONE,
                        LocalDate.parse("2026-10-02"), null), f.auth()));
        error(ErrorCode.VALIDATION_FAILED, () -> service.update(f.bookUid(), created.transferUid(),
                new UpdateTransferRequest(from.getAccountUid(), to.getAccountUid(), BigDecimal.ZERO,
                        LocalDate.parse("2026-10-02"), null), f.auth()));
    }

    @Test
    void monthRangeSortsDateAndUidDescendingAndFetchesBothNames() {
        Fixture f = fixture();
        MoneyBookAccount from = account(f.book(), "cash");
        MoneyBookAccount to = account(f.book(), "bank");
        service.create(f.bookUid(), request(from, to, "1", "2026-09-30"), f.auth());
        var first = service.create(f.bookUid(), request(from, to, "2", "2026-10-02"), f.auth());
        var second = service.create(f.bookUid(), request(from, to, "3", "2026-10-02"), f.auth());
        var latest = service.create(f.bookUid(), request(from, to, "4", "2026-10-31"), f.auth());
        service.create(f.bookUid(), request(from, to, "5", "2026-11-01"), f.auth());
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var october = service.list(f.bookUid(), 2026, 10, f.auth());
        assertEquals(List.of(latest.transferUid(), second.transferUid(), first.transferUid()),
                october.stream().map(row -> row.transferUid()).toList());
        assertTrue(october.stream().allMatch(row -> row.fromAccountName().equals("cash")
                && row.toAccountName().equals("bank")));
        assertTrue(statistics.getPrepareStatementCount() <= 3, "transfer names must not trigger N+1");
        assertTrue(service.list(f.bookUid(), 2026, 12, f.auth()).isEmpty());
    }

    @Test
    void permissionsRequireAcceptedMemberAndMatchingCrudFlag() {
        Fixture f = fixture();
        MoneyBookAccount from = account(f.book(), "cash");
        MoneyBookAccount to = account(f.book(), "bank");
        Long reader = user("reader");
        Long creator = user("creator");
        Long updater = user("updater");
        Long deleter = user("deleter");
        Long pending = user("pending");
        Long rejected = user("rejected");
        member(f.book(), reader, false, false, true, false, false, true);
        member(f.book(), creator, false, true, false, false, false, true);
        member(f.book(), updater, false, false, false, true, false, true);
        member(f.book(), deleter, false, false, false, false, true, true);
        member(f.book(), pending, true, true, true, true, true, false);
        var rejectedMember = member(f.book(), rejected, true, true, true, true, true, false);
        rejectedMember.rejectInvitation();
        var created = service.create(f.bookUid(), request(from, to, "1", "2026-10-02"), auth(creator));
        assertEquals(created.transferUid(), service.detail(f.bookUid(), created.transferUid(), auth(reader)).transferUid());
        error(ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN,
                () -> service.create(f.bookUid(), request(from, to, "1", "2026-10-02"), auth(reader)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> service.detail(f.bookUid(), created.transferUid(), auth(creator)));
        service.update(f.bookUid(), created.transferUid(), new UpdateTransferRequest(
                to.getAccountUid(), from.getAccountUid(), BigDecimal.ONE, LocalDate.parse("2026-10-03"), null), auth(updater));
        error(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN,
                () -> service.update(f.bookUid(), created.transferUid(), new UpdateTransferRequest(
                        from.getAccountUid(), to.getAccountUid(), BigDecimal.ONE,
                        LocalDate.parse("2026-10-02"), null), auth(reader)));
        error(ErrorCode.MONEY_BOOK_DELETE_FORBIDDEN,
                () -> service.delete(f.bookUid(), created.transferUid(), auth(reader)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> service.list(f.bookUid(), 2026, 10, auth(pending)));
        error(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,
                () -> service.list(f.bookUid(), 2026, 10, auth(rejected)));
        service.delete(f.bookUid(), created.transferUid(), auth(deleter));
    }

    private Fixture fixture() {
        Long ownerUid = user("owner");
        return new Fixture(ownerUid, book(ownerUid));
    }

    private Long user(String nickname) {
        return users.save(User.create(nickname, null)).getUserUid();
    }

    private MoneyBook book(Long ownerUid) {
        MoneyBook book = books.save(MoneyBook.create("book", ownerUid));
        memberships.save(MoneyBookUser.owner(book, ownerUid));
        return book;
    }

    private MoneyBookAccount account(MoneyBook book, String name) {
        return accounts.save(MoneyBookAccount.create(book, name, AccountType.BANK, 0));
    }

    private MoneyBookUser member(MoneyBook book, Long uid, boolean admin, boolean create,
                                 boolean read, boolean update, boolean delete, boolean accept) {
        MoneyBookUser member = MoneyBookUser.invite(book, uid, admin, create, read, update, delete);
        if (accept) member.acceptInvitation();
        return memberships.save(member);
    }

    private CreateTransferRequest request(MoneyBookAccount from, MoneyBookAccount to, String amount, String date) {
        return new CreateTransferRequest(from.getAccountUid(), to.getAccountUid(), new BigDecimal(amount),
                LocalDate.parse(date), null);
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build());
    }

    private void error(ErrorCode expected, Runnable action) {
        assertEquals(expected, assertThrows(BusinessException.class, action::run).getErrorCode());
    }

    private record Fixture(Long ownerUid, MoneyBook book) {
        Long bookUid() { return book.getMoneyBookUid(); }
        JwtAuthenticationToken auth() {
            return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "HS256")
                    .claim("sub", ownerUid.toString()).build());
        }
    }
}
