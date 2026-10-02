package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.activity.ActivityRecorder;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.dto.CreateInvitationRequest;
import com.moneybook.backend.moneybook.dto.InvitationResponse;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
import com.moneybook.backend.moneybook.service.impl.MoneyBookServiceImpl;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoneyBookInvitationServiceTests {

    private static final Long BOOK_UID = 7L;
    private static final Long OWNER_UID = 1L;
    private static final Long ACTOR_UID = 42L;
    private static final Long TARGET_UID = 99L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final MoneyBookRepository moneyBookRepository = mock(MoneyBookRepository.class);
    private final MoneyBookUserRepository moneyBookUserRepository = mock(MoneyBookUserRepository.class);
    private final UserAuthRepository userAuthRepository = mock(UserAuthRepository.class);
    private final MoneyBookServiceImpl service = new MoneyBookServiceImpl(
            userRepository, moneyBookRepository, moneyBookUserRepository, userAuthRepository,
            mock(MoneyBookSettingRepository.class), mock(ActivityRecorder.class));

    @Test
    void ownerInvitesLocalUserAndAdminGetsAllPermissions() {
        MoneyBook book = book(ACTOR_UID);
        target(TARGET_UID);
        when(moneyBookUserRepository.save(any())).thenAnswer(invocation -> {
            MoneyBookUser membership = invocation.getArgument(0);
            ReflectionTestUtils.setField(membership, "moneyBookUserUid", 123L);
            return membership;
        });

        InvitationResponse response = service.invite(BOOK_UID,
                request(true, false, false, false, false), authentication(ACTOR_UID));

        assertEquals(123L, response.moneyBookUserUid());
        assertEquals(BOOK_UID, response.moneyBookUid());
        assertEquals(TARGET_UID, response.userUid());
        assertEquals(InvitationStatus.PENDING, response.invitationStatus());
        assertTrue(response.isAdmin());
        assertTrue(response.canCreate());
        assertTrue(response.canRead());
        assertTrue(response.canUpdate());
        assertTrue(response.canDelete());
        verify(moneyBookUserRepository, never()).findByMoneyBookUidAndUserUid(BOOK_UID, ACTOR_UID);
    }

    @Test
    void acceptedAdminCanInviteWithRequestedMemberPermissions() {
        book(OWNER_UID);
        actorMembership(InvitationStatus.ACCEPTED, true);
        target(TARGET_UID);
        when(moneyBookUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvitationResponse response = service.invite(BOOK_UID,
                request(false, true, false, true, false), authentication(ACTOR_UID));

        assertFalse(response.isAdmin());
        assertTrue(response.canCreate());
        assertFalse(response.canRead());
        assertTrue(response.canUpdate());
        assertFalse(response.canDelete());
    }

    @Test
    void ordinaryOrPendingAdminCannotInvite() {
        book(OWNER_UID);
        for (InvitationStatus status : new InvitationStatus[]{InvitationStatus.ACCEPTED, InvitationStatus.PENDING}) {
            actorMembership(status, status == InvitationStatus.PENDING);
            assertError(ErrorCode.MONEY_BOOK_INVITATION_FORBIDDEN,
                    () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                            authentication(ACTOR_UID)));
        }
        verify(userAuthRepository, never()).findByLocalLoginId(any());
    }

    @Test
    void unknownLocalLoginIdCannotBeInvited() {
        book(ACTOR_UID);
        when(userAuthRepository.findByLocalLoginId("target")).thenReturn(Optional.empty());

        assertError(ErrorCode.INVITEE_NOT_FOUND,
                () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                        authentication(ACTOR_UID)));
    }

    @Test
    void selfAndOwnerCannotBeInvited() {
        book(ACTOR_UID);
        target(ACTOR_UID);
        assertError(ErrorCode.SELF_INVITATION,
                () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                        authentication(ACTOR_UID)));

        book(OWNER_UID);
        actorMembership(InvitationStatus.ACCEPTED, true);
        target(OWNER_UID);
        assertError(ErrorCode.ALREADY_MONEY_BOOK_MEMBER,
                () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                        authentication(ACTOR_UID)));
    }

    @Test
    void acceptedAndPendingMembersCannotBeInvitedAgain() {
        book(ACTOR_UID);
        target(TARGET_UID);
        MoneyBookUser existing = mock(MoneyBookUser.class);
        when(moneyBookUserRepository.findByMoneyBookUidAndUserUid(BOOK_UID, TARGET_UID))
                .thenReturn(Optional.of(existing));

        when(existing.getInvitationStatus()).thenReturn(InvitationStatus.ACCEPTED);
        assertError(ErrorCode.ALREADY_MONEY_BOOK_MEMBER,
                () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                        authentication(ACTOR_UID)));
        when(existing.getInvitationStatus()).thenReturn(InvitationStatus.PENDING);
        assertError(ErrorCode.INVITATION_ALREADY_PENDING,
                () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                        authentication(ACTOR_UID)));
        verify(moneyBookUserRepository, never()).save(any());
    }

    @Test
    void rejectedMembershipIsReinvitedUsingTheSameRowAndNewPermissions() {
        MoneyBook book = book(ACTOR_UID);
        target(TARGET_UID);
        MoneyBookUser existing = MoneyBookUser.invite(book, TARGET_UID, false,
                true, true, true, true);
        ReflectionTestUtils.setField(existing, "moneyBookUserUid", 123L);
        existing.rejectInvitation();
        when(moneyBookUserRepository.findByMoneyBookUidAndUserUid(BOOK_UID, TARGET_UID))
                .thenReturn(Optional.of(existing));
        when(moneyBookUserRepository.save(existing)).thenReturn(existing);

        InvitationResponse response = service.invite(BOOK_UID,
                request(false, false, true, false, false), authentication(ACTOR_UID));

        verify(moneyBookUserRepository).save(existing);
        assertSame(existing, moneyBookUserRepository.findByMoneyBookUidAndUserUid(BOOK_UID, TARGET_UID).orElseThrow());
        assertEquals(123L, response.moneyBookUserUid());
        assertEquals(InvitationStatus.PENDING, existing.getInvitationStatus());
        assertFalse(existing.isCanCreate());
        assertTrue(existing.isCanRead());
        assertFalse(existing.isCanUpdate());
        assertFalse(existing.isCanDelete());
    }

    @Test
    void inviteRejectsMissingBook() {
        activeActor();
        when(moneyBookRepository.findById(BOOK_UID)).thenReturn(Optional.empty());

        assertError(ErrorCode.MONEY_BOOK_NOT_FOUND,
                () -> service.invite(BOOK_UID, request(false, true, true, true, false),
                        authentication(ACTOR_UID)));
    }

    @Test
    void ownPendingInvitationCanBeAcceptedOrRejected() {
        MoneyBook book = book(OWNER_UID);
        MoneyBookUser accepted = pending(book, ACTOR_UID, 123L);
        when(moneyBookUserRepository.findById(123L)).thenReturn(Optional.of(accepted));
        InvitationResponse acceptedResponse = service.acceptInvitation(BOOK_UID, 123L, authentication(ACTOR_UID));
        assertEquals(InvitationStatus.ACCEPTED, acceptedResponse.invitationStatus());

        MoneyBookUser rejected = pending(book, ACTOR_UID, 124L);
        when(moneyBookUserRepository.findById(124L)).thenReturn(Optional.of(rejected));
        InvitationResponse rejectedResponse = service.rejectInvitation(BOOK_UID, 124L, authentication(ACTOR_UID));
        assertEquals(InvitationStatus.REJECTED, rejectedResponse.invitationStatus());
    }

    @Test
    void anotherUsersInvitationCannotBeChanged() {
        MoneyBook book = book(OWNER_UID);
        when(moneyBookUserRepository.findById(123L)).thenReturn(Optional.of(pending(book, TARGET_UID, 123L)));

        assertError(ErrorCode.INVITATION_NOT_OWNED,
                () -> service.acceptInvitation(BOOK_UID, 123L, authentication(ACTOR_UID)));
        assertError(ErrorCode.INVITATION_NOT_OWNED,
                () -> service.rejectInvitation(BOOK_UID, 123L, authentication(ACTOR_UID)));
    }

    @Test
    void nonPendingInvitationCannotBeChanged() {
        MoneyBook book = book(OWNER_UID);
        MoneyBookUser accepted = pending(book, ACTOR_UID, 123L);
        accepted.acceptInvitation();
        when(moneyBookUserRepository.findById(123L)).thenReturn(Optional.of(accepted));

        assertError(ErrorCode.INVITATION_NOT_PENDING,
                () -> service.acceptInvitation(BOOK_UID, 123L, authentication(ACTOR_UID)));
        assertError(ErrorCode.INVITATION_NOT_PENDING,
                () -> service.rejectInvitation(BOOK_UID, 123L, authentication(ACTOR_UID)));
    }

    @Test
    void invitationMustBelongToRequestedBook() {
        MoneyBook book = book(OWNER_UID);
        when(moneyBookUserRepository.findById(123L)).thenReturn(Optional.of(pending(book, ACTOR_UID, 123L)));

        assertError(ErrorCode.INVITATION_NOT_FOUND,
                () -> service.acceptInvitation(8L, 123L, authentication(ACTOR_UID)));
        assertError(ErrorCode.INVITATION_NOT_FOUND,
                () -> service.rejectInvitation(8L, 123L, authentication(ACTOR_UID)));
    }

    @Test
    void missingInvitationCannotBeChanged() {
        activeActor();
        when(moneyBookUserRepository.findById(123L)).thenReturn(Optional.empty());

        assertError(ErrorCode.INVITATION_NOT_FOUND,
                () -> service.acceptInvitation(BOOK_UID, 123L, authentication(ACTOR_UID)));
    }

    private MoneyBook book(Long ownerUid) {
        activeActor();
        MoneyBook book = MoneyBook.create("공유 가계부", ownerUid);
        ReflectionTestUtils.setField(book, "moneyBookUid", BOOK_UID);
        when(moneyBookRepository.findById(BOOK_UID)).thenReturn(Optional.of(book));
        return book;
    }

    private void actorMembership(InvitationStatus status, boolean admin) {
        MoneyBookUser membership = mock(MoneyBookUser.class);
        when(membership.getInvitationStatus()).thenReturn(status);
        when(membership.isAdmin()).thenReturn(admin);
        when(moneyBookUserRepository.findByMoneyBookUidAndUserUid(BOOK_UID, ACTOR_UID))
                .thenReturn(Optional.of(membership));
    }

    private void target(Long userUid) {
        User user = mock(User.class);
        when(user.getUserUid()).thenReturn(userUid);
        UserAuth auth = mock(UserAuth.class);
        when(auth.getUser()).thenReturn(user);
        when(userAuthRepository.findByLocalLoginId("target")).thenReturn(Optional.of(auth));
    }

    private MoneyBookUser pending(MoneyBook book, Long userUid, Long membershipUid) {
        MoneyBookUser membership = MoneyBookUser.invite(book, userUid, false,
                true, false, true, false);
        ReflectionTestUtils.setField(membership, "moneyBookUserUid", membershipUid);
        return membership;
    }

    private void activeActor() {
        User actor = mock(User.class);
        when(actor.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ACTOR_UID)).thenReturn(Optional.of(actor));
    }

    private CreateInvitationRequest request(boolean admin, boolean create, boolean read,
                                            boolean update, boolean delete) {
        return new CreateInvitationRequest("target", admin, create, read, update, delete);
    }

    private JwtAuthenticationToken authentication(Long userUid) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", userUid.toString())
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    private void assertError(ErrorCode errorCode, Runnable operation) {
        BusinessException exception = assertThrows(BusinessException.class, operation::run);
        assertEquals(errorCode, exception.getErrorCode());
    }
}
