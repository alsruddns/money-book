package com.moneybook.backend.moneybook.repository;

import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.CreateInvitationRequest;
import com.moneybook.backend.moneybook.dto.InvitationResponse;
import com.moneybook.backend.moneybook.dto.PendingInvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookMemberResponse;
import com.moneybook.backend.moneybook.dto.UpdateMoneyBookMemberPermissionRequest;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private UserAuthRepository userAuthRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPersistsBookAndFullPermissionOwnerMembership() {
        Long ownerUid = userRepository.save(User.create("생성 사용자", null)).getUserUid();
        CreateMoneyBookResponse created = moneyBookService.create(
                new CreateMoneyBookRequest("우리집 가계부"), authentication(ownerUid));
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

    @Test
    void pendingInvitationListExcludesOtherUsersAndAnsweredInvitationsWithoutReadRequirement() {
        Long ownerUid = userRepository.save(User.create("owner", null)).getUserUid();
        Long currentUid = userRepository.save(User.create("current", null)).getUserUid();
        Long otherUid = userRepository.save(User.create("other", null)).getUserUid();
        MoneyBook pendingBook = bookWithOwner("읽기 없는 초대", ownerUid);
        MoneyBook acceptedBook = bookWithOwner("수락됨", ownerUid);
        MoneyBook rejectedBook = bookWithOwner("거절됨", ownerUid);
        MoneyBook otherBook = bookWithOwner("다른 사용자 초대", ownerUid);
        moneyBookUserRepository.save(MoneyBookUser.invite(pendingBook, currentUid,
                false, false, false, false, false));
        MoneyBookUser accepted = MoneyBookUser.invite(acceptedBook, currentUid,
                false, true, true, true, false);
        accepted.acceptInvitation();
        moneyBookUserRepository.save(accepted);
        MoneyBookUser rejected = MoneyBookUser.invite(rejectedBook, currentUid,
                false, true, true, true, false);
        rejected.rejectInvitation();
        moneyBookUserRepository.save(rejected);
        moneyBookUserRepository.save(MoneyBookUser.invite(otherBook, otherUid,
                false, true, true, true, false));

        List<PendingInvitationResponse> result = moneyBookService.pendingInvitations(authentication(currentUid));

        assertEquals(1, result.size());
        assertEquals(pendingBook.getMoneyBookUid(), result.getFirst().moneyBookUid());
        assertEquals("읽기 없는 초대", result.getFirst().moneyBookName());
        assertEquals(ownerUid, result.getFirst().ownerUserUid());
        assertEquals(InvitationStatus.PENDING, result.getFirst().invitationStatus());
        assertFalse(result.getFirst().canRead());
        assertEquals(1, moneyBookService.pendingInvitations(authentication(otherUid)).size());
    }

    @Test
    void rejectedInvitationReusesMembershipAndCanBeAccepted() {
        User owner = userRepository.save(User.create("owner", null));
        User target = userRepository.save(User.create("target", null));
        userAuthRepository.save(UserAuth.local(target, "target-login", "encoded-hash"));
        MoneyBook book = bookWithOwner("공유 가계부", owner.getUserUid());
        CreateInvitationRequest firstRequest = new CreateInvitationRequest(
                "target-login", false, true, false, false, false);

        InvitationResponse first = moneyBookService.invite(
                book.getMoneyBookUid(), firstRequest, authentication(owner.getUserUid()));
        assertEquals(InvitationStatus.PENDING, first.invitationStatus());
        moneyBookService.rejectInvitation(
                book.getMoneyBookUid(), first.moneyBookUserUid(), authentication(target.getUserUid()));
        InvitationResponse renewed = moneyBookService.invite(
                book.getMoneyBookUid(), new CreateInvitationRequest(
                        "target-login", false, false, true, true, false), authentication(owner.getUserUid()));

        assertEquals(first.moneyBookUserUid(), renewed.moneyBookUserUid());
        assertEquals(InvitationStatus.PENDING, renewed.invitationStatus());
        assertTrue(renewed.canRead());
        assertEquals(1, moneyBookService.pendingInvitations(authentication(target.getUserUid())).size());
        InvitationResponse accepted = moneyBookService.acceptInvitation(
                book.getMoneyBookUid(), renewed.moneyBookUserUid(), authentication(target.getUserUid()));
        assertEquals(InvitationStatus.ACCEPTED, accepted.invitationStatus());
        assertEquals(1, moneyBookService.list(authentication(target.getUserUid())).size());
    }

    @Test
    void memberRosterJoinsNicknamesOrdersOwnerFirstAndExcludesInvitations() {
        Long ownerUid = userRepository.save(User.create("owner", null)).getUserUid();
        Long memberUid = userRepository.save(User.create("member", null)).getUserUid();
        Long noReadUid = userRepository.save(User.create("no-read", null)).getUserUid();
        Long pendingUid = userRepository.save(User.create("pending", null)).getUserUid();
        Long rejectedUid = userRepository.save(User.create("rejected", null)).getUserUid();
        MoneyBook book = bookWithOwner("shared", ownerUid);
        MoneyBookUser member = MoneyBookUser.invite(book, memberUid, false, false, true, false, false);
        member.acceptInvitation();
        moneyBookUserRepository.save(member);
        MoneyBookUser noRead = MoneyBookUser.invite(book, noReadUid, false, false, false, false, false);
        noRead.acceptInvitation();
        moneyBookUserRepository.save(noRead);
        moneyBookUserRepository.save(MoneyBookUser.invite(book, pendingUid,
                false, false, true, false, false));
        MoneyBookUser rejected = MoneyBookUser.invite(book, rejectedUid,
                false, false, true, false, false);
        rejected.rejectInvitation();
        moneyBookUserRepository.save(rejected);

        List<MoneyBookMemberResponse> roster = moneyBookService.members(
                book.getMoneyBookUid(), authentication(memberUid));

        assertEquals(List.of(ownerUid, memberUid, noReadUid),
                roster.stream().map(MoneyBookMemberResponse::userUid).toList());
        assertEquals(List.of("owner", "member", "no-read"),
                roster.stream().map(MoneyBookMemberResponse::nickname).toList());
        assertTrue(roster.getFirst().isOwner());
        assertFalse(roster.getLast().isOwner());
        assertFalse(roster.getLast().canRead());
        assertTrue(roster.getFirst().isAdmin());
        assertTrue(roster.getFirst().canCreate());
        assertTrue(roster.getFirst().canRead());
        assertTrue(roster.getFirst().canUpdate());
        assertTrue(roster.getFirst().canDelete());
        assertEquals(roster, moneyBookService.members(book.getMoneyBookUid(), authentication(ownerUid)));
    }

    @Test
    void ownerCanUpdateAndRemoveAcceptedMemberInDatabase() {
        Long ownerUid = userRepository.save(User.create("owner", null)).getUserUid();
        Long memberUid = userRepository.save(User.create("member", null)).getUserUid();
        MoneyBook book = bookWithOwner("shared", ownerUid);
        MoneyBookUser member = MoneyBookUser.invite(book, memberUid, false, false, false, false, false);
        member.acceptInvitation();
        member = moneyBookUserRepository.save(member);
        Long memberId = member.getMoneyBookUserUid();

        moneyBookService.updateMemberPermissions(book.getMoneyBookUid(), memberId,
                new UpdateMoneyBookMemberPermissionRequest(true, false, false, false, false),
                authentication(ownerUid));
        List<MoneyBookMemberResponse> roster = moneyBookService.members(book.getMoneyBookUid(), authentication(ownerUid));
        MoneyBookMemberResponse updated = roster.getLast();
        assertTrue(updated.isAdmin());
        assertTrue(updated.canCreate());
        assertTrue(updated.canRead());
        assertTrue(updated.canUpdate());
        assertTrue(updated.canDelete());

        moneyBookService.removeMember(book.getMoneyBookUid(), memberId, authentication(ownerUid));
        assertTrue(moneyBookUserRepository.findById(memberId).isEmpty());
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

    private JwtAuthenticationToken authentication(Long userUid) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", userUid.toString())
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
