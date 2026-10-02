package com.moneybook.backend.moneybook.service.impl;

import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.MoneyBookSetting;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.enums.WeekStartDay;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.dto.CreateInvitationRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.InvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.dto.PendingInvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookMemberResponse;
import com.moneybook.backend.moneybook.dto.UpdateMoneyBookMemberPermissionRequest;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
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
    private final UserAuthRepository userAuthRepository;
    private final MoneyBookSettingRepository settingRepository;

    /** Saves the workspace and its owner's accepted, full-permission membership atomically. */
    @Override
    @Transactional
    public CreateMoneyBookResponse create(CreateMoneyBookRequest request, Authentication authentication) {
        Long userUid = activeUserUid(authentication);
        MoneyBook moneyBook = moneyBookRepository.save(MoneyBook.create(request.name(), userUid));
        settingRepository.save(MoneyBookSetting.create(moneyBook, WeekStartDay.SUNDAY));
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

    /** Only the owner or an accepted administrator may create or renew an invitation. */
    @Override
    @Transactional
    public InvitationResponse invite(Long moneyBookUid, CreateInvitationRequest request,
                                     Authentication authentication) {
        Long actorUid = activeUserUid(authentication);
        MoneyBook book = moneyBook(moneyBookUid);
        requireManager(book, actorUid, ErrorCode.MONEY_BOOK_INVITATION_FORBIDDEN);

        UserAuth targetAuth = userAuthRepository.findByLocalLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITEE_NOT_FOUND));
        Long targetUid = targetAuth.getUser().getUserUid();
        if (targetUid.equals(actorUid)) {
            throw new BusinessException(ErrorCode.SELF_INVITATION);
        }
        if (targetUid.equals(book.getOwnerUserUid())) {
            throw new BusinessException(ErrorCode.ALREADY_MONEY_BOOK_MEMBER);
        }

        MoneyBookUser membership = moneyBookUserRepository
                .findByMoneyBookUidAndUserUid(moneyBookUid, targetUid)
                .map(existing -> reinvite(existing, request))
                .orElseGet(() -> MoneyBookUser.invite(book, targetUid,
                        request.isAdmin(), request.canCreate(), request.canRead(),
                        request.canUpdate(), request.canDelete()));
        return invitationResponse(moneyBookUserRepository.save(membership));
    }

    /** Pending invitations are visible even when the proposed membership lacks read permission. */
    @Override
    @Transactional(readOnly = true)
    public List<PendingInvitationResponse> pendingInvitations(Authentication authentication) {
        Long userUid = activeUserUid(authentication);
        return moneyBookUserRepository.findPendingByUserUid(userUid).stream()
                .map(membership -> {
                    MoneyBook book = membership.getMoneyBook();
                    return new PendingInvitationResponse(
                            membership.getMoneyBookUserUid(), book.getMoneyBookUid(),
                            book.getName(), book.getOwnerUserUid(), membership.getInvitationStatus(),
                            membership.isAdmin(), membership.isCanCreate(), membership.isCanRead(),
                            membership.isCanUpdate(), membership.isCanDelete());
                })
                .toList();
    }

    @Override
    @Transactional
    public InvitationResponse acceptInvitation(Long moneyBookUid, Long moneyBookUserUid,
                                               Authentication authentication) {
        MoneyBookUser membership = pendingOwnInvitation(moneyBookUid, moneyBookUserUid, authentication);
        membership.acceptInvitation();
        return invitationResponse(membership);
    }

    @Override
    @Transactional
    public InvitationResponse rejectInvitation(Long moneyBookUid, Long moneyBookUserUid,
                                               Authentication authentication) {
        MoneyBookUser membership = pendingOwnInvitation(moneyBookUid, moneyBookUserUid, authentication);
        membership.rejectInvitation();
        return invitationResponse(membership);
    }

    /** Accepted members with read permission can see the roster; the owner retains access independently. */
    @Override
    @Transactional(readOnly = true)
    public List<MoneyBookMemberResponse> members(Long moneyBookUid, Authentication authentication) {
        Long actorUid = activeUserUid(authentication);
        MoneyBook book = moneyBook(moneyBookUid);
        if (!book.getOwnerUserUid().equals(actorUid)) {
            MoneyBookUser actor = moneyBookUserRepository.findByMoneyBookUidAndUserUid(moneyBookUid, actorUid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_MEMBERS_VIEW_FORBIDDEN));
            if (actor.getInvitationStatus() != InvitationStatus.ACCEPTED || !actor.isCanRead()) {
                throw new BusinessException(ErrorCode.MONEY_BOOK_MEMBERS_VIEW_FORBIDDEN);
            }
        }
        return moneyBookUserRepository.findAcceptedMembersByBookUid(moneyBookUid).stream()
                .map(row -> {
                    boolean owner = row.userUid().equals(row.ownerUserUid());
                    return new MoneyBookMemberResponse(
                            row.moneyBookUserUid(), row.moneyBookUid(), row.userUid(), row.nickname(), owner,
                            owner || row.isAdmin(), owner || row.canCreate(), owner || row.canRead(),
                            owner || row.canUpdate(), owner || row.canDelete());
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateMemberPermissions(Long moneyBookUid, Long moneyBookUserUid,
                                        UpdateMoneyBookMemberPermissionRequest request,
                                        Authentication authentication) {
        Long actorUid = activeUserUid(authentication);
        MoneyBook book = moneyBook(moneyBookUid);
        requireManager(book, actorUid, ErrorCode.MONEY_BOOK_MEMBER_PERMISSION_FORBIDDEN);
        MoneyBookUser member = acceptedMember(moneyBookUid, moneyBookUserUid);
        if (book.getOwnerUserUid().equals(member.getUserUid())) {
            throw new BusinessException(ErrorCode.MONEY_BOOK_OWNER_PERMISSION_PROTECTED);
        }
        member.changePermissions(request.isAdmin(), request.canCreate(), request.canRead(),
                request.canUpdate(), request.canDelete());
    }

    @Override
    @Transactional
    public void removeMember(Long moneyBookUid, Long moneyBookUserUid, Authentication authentication) {
        Long actorUid = activeUserUid(authentication);
        MoneyBook book = moneyBook(moneyBookUid);
        requireManager(book, actorUid, ErrorCode.MONEY_BOOK_MEMBER_REMOVAL_FORBIDDEN);
        MoneyBookUser member = acceptedMember(moneyBookUid, moneyBookUserUid);
        if (book.getOwnerUserUid().equals(member.getUserUid())) {
            throw new BusinessException(ErrorCode.MONEY_BOOK_OWNER_REMOVAL_PROTECTED);
        }
        moneyBookUserRepository.delete(member);
    }

    private MoneyBook moneyBook(Long moneyBookUid) {
        return moneyBookRepository.findById(moneyBookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_NOT_FOUND));
    }

    /** The persisted owner UID is authoritative; otherwise current accepted admin membership is required. */
    private void requireManager(MoneyBook book, Long actorUid, ErrorCode forbidden) {
        if (book.getOwnerUserUid().equals(actorUid)) {
            return;
        }
        MoneyBookUser actor = moneyBookUserRepository
                .findByMoneyBookUidAndUserUid(book.getMoneyBookUid(), actorUid)
                .orElseThrow(() -> new BusinessException(forbidden));
        if (actor.getInvitationStatus() != InvitationStatus.ACCEPTED || !actor.isAdmin()) {
            throw new BusinessException(forbidden);
        }
    }

    private MoneyBookUser acceptedMember(Long moneyBookUid, Long moneyBookUserUid) {
        MoneyBookUser member = moneyBookUserRepository.findById(moneyBookUserUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_MEMBER_NOT_FOUND));
        if (!member.getMoneyBook().getMoneyBookUid().equals(moneyBookUid)) {
            throw new BusinessException(ErrorCode.MONEY_BOOK_MEMBER_NOT_FOUND);
        }
        if (member.getInvitationStatus() != InvitationStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.MONEY_BOOK_MEMBER_NOT_ACCEPTED);
        }
        return member;
    }

    private MoneyBookUser reinvite(MoneyBookUser membership, CreateInvitationRequest request) {
        if (membership.getInvitationStatus() == InvitationStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.ALREADY_MONEY_BOOK_MEMBER);
        }
        if (membership.getInvitationStatus() == InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PENDING);
        }
        membership.reinvite(request.isAdmin(), request.canCreate(), request.canRead(),
                request.canUpdate(), request.canDelete());
        return membership;
    }

    private MoneyBookUser pendingOwnInvitation(Long moneyBookUid, Long moneyBookUserUid,
                                               Authentication authentication) {
        Long userUid = activeUserUid(authentication);
        MoneyBookUser membership = moneyBookUserRepository.findById(moneyBookUserUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        if (!membership.getMoneyBook().getMoneyBookUid().equals(moneyBookUid)) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }
        if (!membership.getUserUid().equals(userUid)) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_OWNED);
        }
        if (membership.getInvitationStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_PENDING);
        }
        return membership;
    }

    private InvitationResponse invitationResponse(MoneyBookUser membership) {
        return new InvitationResponse(
                membership.getMoneyBookUserUid(), membership.getMoneyBook().getMoneyBookUid(),
                membership.getUserUid(), membership.getInvitationStatus(), membership.isAdmin(),
                membership.isCanCreate(), membership.isCanRead(),
                membership.isCanUpdate(), membership.isCanDelete());
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
