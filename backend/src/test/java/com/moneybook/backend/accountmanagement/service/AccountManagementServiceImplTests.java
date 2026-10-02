package com.moneybook.backend.accountmanagement.service;

import com.moneybook.backend.accountmanagement.dto.AccountPasswordUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountProfileUpdateReqDto;
import com.moneybook.backend.accountmanagement.service.impl.AccountManagementServiceImpl;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountManagementServiceImplTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAuthRepository userAuthRepository = mock(UserAuthRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AccountManagementServiceImpl service = new AccountManagementServiceImpl(
            userRepository, userAuthRepository, passwordEncoder);
    private User user;

    @BeforeEach
    void setUp() {
        user = spy(User.create("기존 이름", null));
        when(user.getUserUid()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of());
    }

    @Test
    void meReturnsProfileAndProviderWithoutCredentials() {
        UserAuth localAuth = UserAuth.local(user, "member-id", passwordEncoder.encode("old-password"));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(localAuth));

        var response = service.me(authentication());

        assertEquals(42L, response.userUid());
        assertEquals("기존 이름", response.nickname());
        assertEquals(List.of(AuthProvider.LOCAL), response.providers());
        assertEquals("member-id", response.loginId());
        assertFalse(response.toString().contains("old-password"));
        assertFalse(response.toString().contains(localAuth.getPasswordHash()));
        assertFalse(response.toString().contains("providerUserId"));
    }

    @Test
    void profileUpdateTrimsNicknameAndSameValueIsNoOp() {
        var changed = service.updateProfile(authentication(), new AccountProfileUpdateReqDto("  새 이름  "));
        assertEquals("새 이름", changed.nickname());

        var unchanged = service.updateProfile(authentication(), new AccountProfileUpdateReqDto("새 이름"));
        assertEquals("새 이름", unchanged.nickname());
        verify(userRepository, never()).save(any());
    }

    @Test
    void passwordUpdateStoresOnlyBcryptHash() {
        UserAuth localAuth = UserAuth.local(user, "member-id", passwordEncoder.encode("old-password"));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(localAuth));

        service.updatePassword(authentication(),
                new AccountPasswordUpdateReqDto("old-password", "new-password", "new-password"));

        assertTrue(passwordEncoder.matches("new-password", localAuth.getPasswordHash()));
        assertFalse("new-password".equals(localAuth.getPasswordHash()));
        verify(userAuthRepository).save(localAuth);
    }

    @Test
    void passwordUpdateRejectsWrongCurrentPasswordWithoutSaving() {
        UserAuth localAuth = UserAuth.local(user, "member-id", passwordEncoder.encode("old-password"));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(localAuth));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updatePassword(
                authentication(), new AccountPasswordUpdateReqDto("wrong", "new-password", "new-password")));

        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void passwordUpdateRejectsMismatchedConfirmation() {
        UserAuth localAuth = UserAuth.local(user, "member-id", passwordEncoder.encode("old-password"));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(localAuth));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updatePassword(
                authentication(), new AccountPasswordUpdateReqDto("old-password", "new-password", "different")));

        assertEquals(ErrorCode.PASSWORD_CONFIRM_MISMATCH, exception.getErrorCode());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void passwordUpdateRejectsSamePassword() {
        UserAuth localAuth = UserAuth.local(user, "member-id", passwordEncoder.encode("old-password"));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(localAuth));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updatePassword(
                authentication(), new AccountPasswordUpdateReqDto("old-password", "old-password", "old-password")));

        assertEquals(ErrorCode.SAME_AS_CURRENT_PASSWORD, exception.getErrorCode());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void passwordUpdateRejectsUtf8PasswordOverBcryptLimit() {
        UserAuth localAuth = UserAuth.local(user, "member-id", passwordEncoder.encode("old-password"));
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(localAuth));
        String tooLong = "가".repeat(25);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updatePassword(
                authentication(), new AccountPasswordUpdateReqDto("old-password", tooLong, tooLong)));

        assertEquals(ErrorCode.INVALID_PASSWORD_LENGTH, exception.getErrorCode());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void passwordUpdateRejectsUserWithoutLocalAuth() {
        UserAuth oauthAuth = UserAuth.oauth(user, AuthProvider.GOOGLE, "provider-id");
        when(userAuthRepository.findByUserUid(42L)).thenReturn(List.of(oauthAuth));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updatePassword(
                authentication(), new AccountPasswordUpdateReqDto("current", "new-password", "new-password")));

        assertEquals(ErrorCode.LOCAL_AUTH_NOT_FOUND, exception.getErrorCode());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void passwordRequestDoesNotExposeSecretsInToString() {
        var request = new AccountPasswordUpdateReqDto("current-secret", "new-secret", "new-secret");

        assertEquals("AccountPasswordUpdateReqDto[passwords=REDACTED]", request.toString());
    }

    private JwtAuthenticationToken authentication() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256").claim("sub", "42").build();
        return new JwtAuthenticationToken(jwt);
    }
}
