package com.moneybook.backend.accountmanagement.service.impl;

import com.moneybook.backend.accountmanagement.dto.AccountMeResDto;
import com.moneybook.backend.accountmanagement.dto.AccountPasswordUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountProfileUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountWithdrawalRequest;
import com.moneybook.backend.accountmanagement.service.AccountManagementService;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountManagementServiceImpl implements AccountManagementService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final MoneyBookRepository moneyBookRepository;
    private final MoneyBookUserRepository moneyBookUserRepository;

    /** Returns the authenticated user's current profile and non-secret provider information. */
    @Override
    @Transactional(readOnly = true)
    public AccountMeResDto me(Authentication authentication) {
        return toResponse(requireActiveUser(authentication));
    }

    /** Trims and updates only the authenticated user's nickname; unchanged values are a no-op. */
    @Override
    @Transactional
    public AccountMeResDto updateProfile(Authentication authentication, AccountProfileUpdateReqDto request) {
        User user = requireActiveUser(authentication);
        String nickname = request.nickname().strip();
        if (!user.getNickname().equals(nickname)) {
            user.changeNickname(nickname);
        }
        return toResponse(user);
    }

    /** Re-authenticates a LOCAL user and persists only the BCrypt hash of the new password. */
    @Override
    @Transactional
    public AccountMeResDto updatePassword(Authentication authentication, AccountPasswordUpdateReqDto request) {
        User user = requireActiveUser(authentication);
        UserAuth localAuth = userAuthRepository.findByUserUid(user.getUserUid()).stream()
                .filter(userAuth -> userAuth.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCAL_AUTH_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), localAuth.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_LENGTH);
        }
        if (passwordEncoder.matches(request.newPassword(), localAuth.getPasswordHash())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        localAuth.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        userAuthRepository.save(localAuth);
        return toResponse(user);
    }

    /** Re-authenticates a LOCAL user, refuses owned books and anonymizes the retained account row. */
    @Override
    @Transactional
    public void withdraw(Authentication authentication, AccountWithdrawalRequest request) {
        Long userUid = authenticatedUserUid(authentication);
        User user = userRepository.findByIdForUpdate(userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_WITHDRAWAL_FORBIDDEN);
        }

        UserAuth localAuth = userAuthRepository.findByUserUid(userUid).stream()
                .filter(userAuth -> userAuth.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCAL_AUTH_NOT_FOUND));
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_LENGTH);
        }
        if (!passwordEncoder.matches(request.currentPassword(), localAuth.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (moneyBookRepository.countOwnedByUserUid(userUid) > 0) {
            throw new BusinessException(ErrorCode.OWNED_MONEY_BOOK_EXISTS);
        }

        // Invitation states are stored in membership rows; all are removed while immutable activity snapshots remain.
        moneyBookUserRepository.deleteAllByUserUid(userUid);
        userAuthRepository.deleteAllByUserUid(userUid);
        user.withdraw();
    }

    private User requireActiveUser(Authentication authentication) {
        Long userUid = authenticatedUserUid(authentication);

        User user = userRepository.findById(userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        return user;
    }

    private Long authenticatedUserUid(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private AccountMeResDto toResponse(User user) {
        List<UserAuth> auths = userAuthRepository.findByUserUid(user.getUserUid());
        List<AuthProvider> providers = auths.stream().map(UserAuth::getProvider).distinct().toList();
        String localLoginId = auths.stream()
                .filter(userAuth -> userAuth.getProvider() == AuthProvider.LOCAL)
                .map(UserAuth::getLoginId)
                .findFirst()
                .orElse(null);
        return new AccountMeResDto(user.getUserUid(), user.getNickname(), user.getStatus(), user.getSystemRole(),
                providers, localLoginId, user.getRegTime(), user.getModTime());
    }
}
