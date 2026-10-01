package com.moneybook.backend.moneybook.service.impl;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.moneybook.service.MoneyBookService;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoneyBookServiceImpl implements MoneyBookService {

    private final UserRepository userRepository;
    private final MoneyBookRepository moneyBookRepository;
    private final MoneyBookUserRepository moneyBookUserRepository;

    /** Saves the workspace and its owner's accepted, full-permission membership atomically. */
    @Override
    @Transactional
    public CreateMoneyBookResponse create(CreateMoneyBookRequest request, Authentication authentication) {
        Long userUid = activeUserUid(authentication);
        MoneyBook moneyBook = moneyBookRepository.save(MoneyBook.create(request.name(), userUid));
        moneyBookUserRepository.save(MoneyBookUser.owner(moneyBook, userUid));
        return new CreateMoneyBookResponse(moneyBook.getMoneyBookUid(), moneyBook.getName(), userUid);
    }

    /** Uses accepted, readable memberships as the sole source of accessible workspaces. */
    @Override
    @Transactional(readOnly = true)
    public List<MoneyBookListResponse> list(Authentication authentication) {
        Long userUid = activeUserUid(authentication);
        return moneyBookUserRepository.findReadableAcceptedByUserUid(userUid).stream()
                .map(membership -> {
                    MoneyBook book = membership.getMoneyBook();
                    return new MoneyBookListResponse(
                            book.getMoneyBookUid(), book.getName(), book.getOwnerUserUid(),
                            book.getOwnerUserUid().equals(userUid), membership.isAdmin(),
                            membership.isCanCreate(), membership.isCanRead(),
                            membership.isCanUpdate(), membership.isCanDelete());
                })
                .toList();
    }

    private Long activeUserUid(Authentication authentication) {
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
}
