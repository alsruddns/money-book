package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.moneybook.service.impl.MoneyBookServiceImpl;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MoneyBookServiceImplTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final MoneyBookRepository moneyBookRepository = mock(MoneyBookRepository.class);
    private final MoneyBookUserRepository moneyBookUserRepository = mock(MoneyBookUserRepository.class);
    private final UserAuthRepository userAuthRepository = mock(UserAuthRepository.class);
    private final MoneyBookServiceImpl service = new MoneyBookServiceImpl(
            userRepository, moneyBookRepository, moneyBookUserRepository, userAuthRepository);

    @Test
    void createSavesBookAndAcceptedOwnerMembershipWithAllPermissions() {
        activeUser(42L);
        when(moneyBookRepository.save(any())).thenAnswer(invocation -> {
            MoneyBook book = invocation.getArgument(0);
            ReflectionTestUtils.setField(book, "moneyBookUid", 7L);
            return book;
        });

        CreateMoneyBookResponse response = service.create(
                new CreateMoneyBookRequest("우리집 가계부"), authentication("42"));

        ArgumentCaptor<MoneyBook> bookCaptor = ArgumentCaptor.forClass(MoneyBook.class);
        ArgumentCaptor<MoneyBookUser> memberCaptor = ArgumentCaptor.forClass(MoneyBookUser.class);
        verify(moneyBookRepository).save(bookCaptor.capture());
        verify(moneyBookUserRepository).save(memberCaptor.capture());
        MoneyBook book = bookCaptor.getValue();
        MoneyBookUser member = memberCaptor.getValue();
        assertEquals(7L, response.moneyBookUid());
        assertEquals("우리집 가계부", response.name());
        assertEquals(42L, response.ownerUserUid());
        assertEquals(42L, book.getOwnerUserUid());
        assertSame(book, member.getMoneyBook());
        assertEquals(42L, member.getUserUid());
        assertTrue(member.isAdmin());
        assertTrue(member.isCanCreate());
        assertTrue(member.isCanRead());
        assertTrue(member.isCanUpdate());
        assertTrue(member.isCanDelete());
        assertEquals(InvitationStatus.ACCEPTED, member.getInvitationStatus());
    }

    @Test
    void createRollsBackBothSavesWhenOwnerMembershipFails() {
        activeUser(42L);
        when(moneyBookRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moneyBookUserRepository.save(any())).thenThrow(new IllegalStateException("membership save failed"));
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager, new AnnotationTransactionAttributeSource()));
        MoneyBookService transactionalService = (MoneyBookService) proxyFactory.getProxy();

        assertThrows(IllegalStateException.class, () -> transactionalService.create(
                new CreateMoneyBookRequest("우리집 가계부"), authentication("42")));

        verify(moneyBookRepository).save(any());
        verify(moneyBookUserRepository).save(any());
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void listUsesCurrentUsersReadableAcceptedMemberships() {
        activeUser(42L);
        MoneyBook book = MoneyBook.create("우리집 가계부", 42L);
        ReflectionTestUtils.setField(book, "moneyBookUid", 7L);
        when(moneyBookUserRepository.findReadableAcceptedByUserUid(42L))
                .thenReturn(List.of(MoneyBookUser.owner(book, 42L)));

        List<MoneyBookListResponse> response = service.list(authentication("42"));

        verify(moneyBookUserRepository).findReadableAcceptedByUserUid(42L);
        assertEquals(1, response.size());
        assertEquals(7L, response.getFirst().moneyBookUid());
        assertTrue(response.getFirst().isOwner());
        assertTrue(response.getFirst().isAdmin());
        assertTrue(response.getFirst().canRead());
    }

    @Test
    void listWithoutMembershipsReturnsEmptyArray() {
        activeUser(42L);
        when(moneyBookUserRepository.findReadableAcceptedByUserUid(42L)).thenReturn(List.of());

        assertTrue(service.list(authentication("42")).isEmpty());
    }

    @Test
    void listKeepsOwnerIdentitySeparateFromAdminPermission() {
        activeUser(42L);
        MoneyBook book = MoneyBook.create("공유 가계부", 99L);
        ReflectionTestUtils.setField(book, "moneyBookUid", 8L);
        MoneyBookUser membership = mock(MoneyBookUser.class);
        when(membership.getMoneyBook()).thenReturn(book);
        when(membership.isAdmin()).thenReturn(true);
        when(membership.isCanRead()).thenReturn(true);
        when(moneyBookUserRepository.findReadableAcceptedByUserUid(42L))
                .thenReturn(List.of(membership));

        MoneyBookListResponse response = service.list(authentication("42")).getFirst();

        assertEquals(99L, response.ownerUserUid());
        assertFalse(response.isOwner());
        assertTrue(response.isAdmin());
    }

    @Test
    void createRejectsMissingUserBeforeSaving() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(new CreateMoneyBookRequest("우리집 가계부"), authentication("42")));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(moneyBookRepository, moneyBookUserRepository);
    }

    @Test
    void createRejectsInactiveUserBeforeSaving() {
        User user = mock(User.class);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(new CreateMoneyBookRequest("우리집 가계부"), authentication("42")));

        assertEquals(ErrorCode.USER_INACTIVE, exception.getErrorCode());
        verifyNoInteractions(moneyBookRepository, moneyBookUserRepository);
    }

    private void activeUser(Long userUid) {
        User user = mock(User.class);
        when(userRepository.findById(userUid)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
    }

    private JwtAuthenticationToken authentication(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", subject)
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
