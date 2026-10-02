package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.activity.ActivityRecorder;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.dto.UpdateMoneyBookMemberPermissionRequest;
import com.moneybook.backend.moneybook.repository.MoneyBookMemberRow;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
import com.moneybook.backend.moneybook.service.impl.MoneyBookServiceImpl;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoneyBookMemberServiceTests {

    private static final Long BOOK = 7L;
    private static final Long OWNER = 1L;
    private static final Long ACTOR = 2L;
    private static final Long TARGET = 3L;
    private static final Long MEMBER_ID = 11L;

    private final UserRepository users = mock(UserRepository.class);
    private final MoneyBookRepository books = mock(MoneyBookRepository.class);
    private final MoneyBookUserRepository memberships = mock(MoneyBookUserRepository.class);
    private final MoneyBookServiceImpl service = new MoneyBookServiceImpl(
            users, books, memberships, mock(UserAuthRepository.class), mock(MoneyBookSettingRepository.class),
            mock(ActivityRecorder.class));

    @Test
    void ownerAndReadableAcceptedMemberCanViewRosterWithProtectedOwnerRights() {
        book();
        when(memberships.findAcceptedMembersByBookUid(BOOK)).thenReturn(List.of(
                new MoneyBookMemberRow(10L, BOOK, OWNER, "owner", OWNER,
                        false, false, false, false, false),
                new MoneyBookMemberRow(MEMBER_ID, BOOK, ACTOR, "actor", OWNER,
                        false, true, true, false, false)));
        MoneyBookUser actor = accepted(book(), ACTOR, MEMBER_ID, false, true);
        when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR)).thenReturn(Optional.of(actor));

        var ownerRoster = service.members(BOOK, auth(OWNER));
        var memberRoster = service.members(BOOK, auth(ACTOR));
        assertEquals(ownerRoster, memberRoster);
        assertTrue(ownerRoster.getFirst().isOwner());
        assertTrue(ownerRoster.getFirst().isAdmin());
        assertTrue(ownerRoster.getFirst().canCreate());
        assertTrue(ownerRoster.getFirst().canRead());
        assertTrue(ownerRoster.getFirst().canUpdate());
        assertTrue(ownerRoster.getFirst().canDelete());
        assertFalse(ownerRoster.getLast().isOwner());
    }

    @Test
    void otherUserUnreadableMemberAndPendingMemberCannotViewRoster() {
        MoneyBook book = book();
        error(ErrorCode.MONEY_BOOK_MEMBERS_VIEW_FORBIDDEN, () -> service.members(BOOK, auth(ACTOR)));
        for (MoneyBookUser member : List.of(
                accepted(book, ACTOR, MEMBER_ID, false, false),
                pending(book, ACTOR, MEMBER_ID))) {
            when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR)).thenReturn(Optional.of(member));
            error(ErrorCode.MONEY_BOOK_MEMBERS_VIEW_FORBIDDEN, () -> service.members(BOOK, auth(ACTOR)));
        }
        verify(memberships, never()).findAcceptedMembersByBookUid(BOOK);
    }

    @Test
    void ownerAndAcceptedAdminCanChangePermissionsAndAdminForcesEveryRight() {
        MoneyBook book = book();
        MoneyBookUser target = accepted(book, TARGET, MEMBER_ID, false, false);
        when(memberships.findById(MEMBER_ID)).thenReturn(Optional.of(target));
        service.updateMemberPermissions(BOOK, MEMBER_ID, request(true), auth(OWNER));
        assertTrue(target.isAdmin());
        assertTrue(target.isCanCreate());
        assertTrue(target.isCanRead());
        assertTrue(target.isCanUpdate());
        assertTrue(target.isCanDelete());

        when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR))
                .thenReturn(Optional.of(accepted(book, ACTOR, 12L, true, true)));
        service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(ACTOR));
        assertFalse(target.isAdmin());
        assertFalse(target.isCanCreate());
        assertFalse(target.isCanRead());
        assertFalse(target.isCanUpdate());
        assertFalse(target.isCanDelete());
    }

    @Test
    void ordinaryMemberCannotManageMembers() {
        MoneyBook book = book();
        when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR))
                .thenReturn(Optional.of(accepted(book, ACTOR, 12L, false, true)));
        error(ErrorCode.MONEY_BOOK_MEMBER_PERMISSION_FORBIDDEN,
                () -> service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(ACTOR)));
        error(ErrorCode.MONEY_BOOK_MEMBER_REMOVAL_FORBIDDEN,
                () -> service.removeMember(BOOK, MEMBER_ID, auth(ACTOR)));
        verify(memberships, never()).findById(MEMBER_ID);
    }

    @Test
    void ownerMembershipCannotBeChangedOrRemovedEvenByAdmin() {
        MoneyBook book = book();
        when(memberships.findById(MEMBER_ID))
                .thenReturn(Optional.of(accepted(book, OWNER, MEMBER_ID, true, true)));
        when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR))
                .thenReturn(Optional.of(accepted(book, ACTOR, 12L, true, true)));
        for (Long actor : List.of(OWNER, ACTOR)) {
            error(ErrorCode.MONEY_BOOK_OWNER_PERMISSION_PROTECTED,
                    () -> service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(actor)));
            error(ErrorCode.MONEY_BOOK_OWNER_REMOVAL_PROTECTED,
                    () -> service.removeMember(BOOK, MEMBER_ID, auth(actor)));
        }
        verify(memberships, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingCrossBookAndPendingMembershipsCannotBeManaged() {
        MoneyBook book = book();
        error(ErrorCode.MONEY_BOOK_MEMBER_NOT_FOUND,
                () -> service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(OWNER)));
        error(ErrorCode.MONEY_BOOK_MEMBER_NOT_FOUND,
                () -> service.removeMember(BOOK, MEMBER_ID, auth(OWNER)));

        MoneyBook otherBook = MoneyBook.create("other", OWNER);
        ReflectionTestUtils.setField(otherBook, "moneyBookUid", 8L);
        when(memberships.findById(MEMBER_ID))
                .thenReturn(Optional.of(accepted(otherBook, TARGET, MEMBER_ID, false, true)));
        error(ErrorCode.MONEY_BOOK_MEMBER_NOT_FOUND,
                () -> service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(OWNER)));
        error(ErrorCode.MONEY_BOOK_MEMBER_NOT_FOUND,
                () -> service.removeMember(BOOK, MEMBER_ID, auth(OWNER)));

        when(memberships.findById(MEMBER_ID)).thenReturn(Optional.of(pending(book, TARGET, MEMBER_ID)));
        error(ErrorCode.MONEY_BOOK_MEMBER_NOT_ACCEPTED,
                () -> service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(OWNER)));
        error(ErrorCode.MONEY_BOOK_MEMBER_NOT_ACCEPTED,
                () -> service.removeMember(BOOK, MEMBER_ID, auth(OWNER)));
    }

    @Test
    void ownerAndAdminCanRemoveOrdinaryMembers() {
        MoneyBook book = book();
        MoneyBookUser target = accepted(book, TARGET, MEMBER_ID, true, true);
        when(memberships.findById(MEMBER_ID)).thenReturn(Optional.of(target));
        service.removeMember(BOOK, MEMBER_ID, auth(OWNER));
        when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR))
                .thenReturn(Optional.of(accepted(book, ACTOR, 12L, true, true)));
        service.removeMember(BOOK, MEMBER_ID, auth(ACTOR));
        verify(memberships, org.mockito.Mockito.times(2)).delete(target);
    }

    @Test
    void adminSelfDemotionRemovesManagementPermissionImmediately() {
        MoneyBook book = book();
        MoneyBookUser admin = accepted(book, ACTOR, MEMBER_ID, true, true);
        when(memberships.findByMoneyBookUidAndUserUid(BOOK, ACTOR)).thenReturn(Optional.of(admin));
        when(memberships.findById(MEMBER_ID)).thenReturn(Optional.of(admin));
        service.updateMemberPermissions(BOOK, MEMBER_ID, request(false), auth(ACTOR));
        error(ErrorCode.MONEY_BOOK_MEMBER_REMOVAL_FORBIDDEN,
                () -> service.removeMember(BOOK, MEMBER_ID, auth(ACTOR)));
    }

    private MoneyBook book() {
        for (Long uid : List.of(OWNER, ACTOR)) {
            User user = mock(User.class);
            when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
            when(users.findById(uid)).thenReturn(Optional.of(user));
        }
        MoneyBook book = MoneyBook.create("shared", OWNER);
        ReflectionTestUtils.setField(book, "moneyBookUid", BOOK);
        when(books.findById(BOOK)).thenReturn(Optional.of(book));
        return book;
    }

    private MoneyBookUser pending(MoneyBook book, Long uid, Long memberUid) {
        MoneyBookUser member = MoneyBookUser.invite(book, uid, false, false, false, false, false);
        ReflectionTestUtils.setField(member, "moneyBookUserUid", memberUid);
        return member;
    }

    private MoneyBookUser accepted(MoneyBook book, Long uid, Long memberUid, boolean admin, boolean read) {
        MoneyBookUser member = MoneyBookUser.invite(book, uid, admin, false, read, false, false);
        member.acceptInvitation();
        ReflectionTestUtils.setField(member, "moneyBookUserUid", memberUid);
        return member;
    }

    private UpdateMoneyBookMemberPermissionRequest request(boolean admin) {
        return new UpdateMoneyBookMemberPermissionRequest(admin, false, false, false, false);
    }

    private JwtAuthenticationToken auth(Long uid) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "HS256")
                .claim("sub", uid.toString()).build();
        return new JwtAuthenticationToken(jwt);
    }

    private void error(ErrorCode expected, Runnable operation) {
        assertEquals(expected, assertThrows(BusinessException.class, operation::run).getErrorCode());
    }
}
