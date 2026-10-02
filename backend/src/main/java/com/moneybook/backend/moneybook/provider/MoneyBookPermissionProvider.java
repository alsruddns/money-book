package com.moneybook.backend.moneybook.provider;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Authorizes ledger operations against the current user, owner UID and accepted membership. */
@Component
@RequiredArgsConstructor
public class MoneyBookPermissionProvider {

    private final UserRepository userRepository;
    private final MoneyBookRepository moneyBookRepository;
    private final MoneyBookUserRepository moneyBookUserRepository;

    public MoneyBook require(Long moneyBookUid, Authentication authentication, MoneyBookPermission permission) {
        Long userUid = currentActiveUserUid(authentication);
        MoneyBook book = moneyBookRepository.findById(moneyBookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_NOT_FOUND));
        if (book.getOwnerUserUid().equals(userUid)) {
            return book;
        }
        MoneyBookUser membership = moneyBookUserRepository.findByMoneyBookUidAndUserUid(moneyBookUid, userUid)
                .orElseThrow(() -> new BusinessException(forbidden(permission)));
        if (membership.getInvitationStatus() != InvitationStatus.ACCEPTED || !allowed(membership, permission)) {
            throw new BusinessException(forbidden(permission));
        }
        return book;
    }

    /** Financial backup access is limited to the owner or an accepted administrator. */
    public MoneyBook requireBackupAccess(Long moneyBookUid, Authentication authentication) {
        Long userUid = currentActiveUserUid(authentication);
        MoneyBook book = moneyBookRepository.findById(moneyBookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_NOT_FOUND));
        if (book.getOwnerUserUid().equals(userUid)) return book;
        MoneyBookUser membership = moneyBookUserRepository.findByMoneyBookUidAndUserUid(moneyBookUid, userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_BACKUP_FORBIDDEN));
        if (membership.getInvitationStatus() != InvitationStatus.ACCEPTED || !membership.isAdmin()) {
            throw new BusinessException(ErrorCode.MONEY_BOOK_BACKUP_FORBIDDEN);
        }
        return book;
    }

    /** Returns the active UID from a verified access-token authentication for restore ownership/auditing. */
    public Long currentActiveUserUid(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        Long userUid;
        try {
            userUid = Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        User user = userRepository.findById(userUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        return userUid;
    }

    private boolean allowed(MoneyBookUser membership, MoneyBookPermission permission) {
        return switch (permission) {
            case CREATE -> membership.isCanCreate();
            case READ -> membership.isCanRead();
            case UPDATE -> membership.isCanUpdate();
            case DELETE -> membership.isCanDelete();
        };
    }

    private ErrorCode forbidden(MoneyBookPermission permission) {
        return switch (permission) {
            case CREATE -> ErrorCode.MONEY_BOOK_CREATE_FORBIDDEN;
            case READ -> ErrorCode.MONEY_BOOK_READ_FORBIDDEN;
            case UPDATE -> ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN;
            case DELETE -> ErrorCode.MONEY_BOOK_DELETE_FORBIDDEN;
        };
    }
}
