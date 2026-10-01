package com.moneybook.backend.moneybook.repository;

import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.service.MoneyBookService;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:money_book_query;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class MoneyBookMembershipQueryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MoneyBookRepository moneyBookRepository;

    @Autowired
    private MoneyBookUserRepository moneyBookUserRepository;

    @Autowired
    private MoneyBookService moneyBookService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPersistsBookAndFullPermissionOwnerMembership() {
        Long ownerUid = userRepository.save(User.create("생성 사용자", null)).getUserUid();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", ownerUid.toString())
                .build();

        CreateMoneyBookResponse created = moneyBookService.create(
                new CreateMoneyBookRequest("우리집 가계부"), new JwtAuthenticationToken(jwt));
        List<MoneyBookUser> memberships = moneyBookUserRepository.findReadableAcceptedByUserUid(ownerUid);

        assertEquals(ownerUid, created.ownerUserUid());
        assertEquals(1, memberships.size());
        MoneyBookUser owner = memberships.getFirst();
        assertEquals(created.moneyBookUid(), owner.getMoneyBook().getMoneyBookUid());
        assertEquals(ownerUid, owner.getMoneyBook().getOwnerUserUid());
        assertEquals(InvitationStatus.ACCEPTED, owner.getInvitationStatus());
        assertTrue(owner.isAdmin());
        assertTrue(owner.isCanCreate());
        assertTrue(owner.isCanRead());
        assertTrue(owner.isCanUpdate());
        assertTrue(owner.isCanDelete());
    }

    @Test
    void returnsOnlyCurrentUsersAcceptedReadableBooksInDescendingUidOrder() {
        Long currentUid = userRepository.save(User.create("현재 사용자", null)).getUserUid();
        Long otherUid = userRepository.save(User.create("다른 사용자", null)).getUserUid();
        Long noMembershipUid = userRepository.save(User.create("빈 사용자", null)).getUserUid();

        MoneyBook own = bookWithOwner("내 가계부", currentUid);
        MoneyBook shared = bookWithOwner("공유 가계부", otherUid);
        MoneyBook noRead = bookWithOwner("읽기 불가", otherUid);
        MoneyBook pending = bookWithOwner("초대 대기", otherUid);
        MoneyBook rejected = bookWithOwner("초대 거절", otherUid);
        bookWithOwner("다른 사용자 가계부", otherUid);

        addMember(shared, currentUid, true, "ACCEPTED");
        addMember(noRead, currentUid, false, "ACCEPTED");
        addMember(pending, currentUid, true, "PENDING");
        addMember(rejected, currentUid, true, "REJECTED");

        List<MoneyBookUser> result = moneyBookUserRepository.findReadableAcceptedByUserUid(currentUid);

        assertEquals(List.of(shared.getMoneyBookUid(), own.getMoneyBookUid()),
                result.stream().map(member -> member.getMoneyBook().getMoneyBookUid()).toList());
        assertTrue(moneyBookUserRepository.findReadableAcceptedByUserUid(noMembershipUid).isEmpty());
    }

    private MoneyBook bookWithOwner(String name, Long ownerUid) {
        MoneyBook book = moneyBookRepository.save(MoneyBook.create(name, ownerUid));
        moneyBookUserRepository.save(MoneyBookUser.owner(book, ownerUid));
        return book;
    }

    private void addMember(MoneyBook book, Long userUid, boolean canRead, String status) {
        jdbcTemplate.update("""
                INSERT INTO money_book_users
                    (money_book_uid, user_uid, is_admin, can_create, can_read, can_update,
                     can_delete, invitation_status, reg_time, mod_time)
                VALUES (?, ?, FALSE, FALSE, ?, FALSE, FALSE, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, book.getMoneyBookUid(), userUid, canRead, status);
    }
}
