package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.CreateInvitationRequest;
import com.moneybook.backend.moneybook.dto.InvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.dto.PendingInvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookMemberResponse;
import com.moneybook.backend.moneybook.dto.UpdateMoneyBookMemberPermissionRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MoneyBookService {

    CreateMoneyBookResponse create(CreateMoneyBookRequest request, Authentication authentication);

    List<MoneyBookListResponse> list(Authentication authentication);

    InvitationResponse invite(Long moneyBookUid, CreateInvitationRequest request, Authentication authentication);

    List<PendingInvitationResponse> pendingInvitations(Authentication authentication);

    InvitationResponse acceptInvitation(Long moneyBookUid, Long moneyBookUserUid, Authentication authentication);

    InvitationResponse rejectInvitation(Long moneyBookUid, Long moneyBookUserUid, Authentication authentication);

    List<MoneyBookMemberResponse> members(Long moneyBookUid, Authentication authentication);

    void updateMemberPermissions(Long moneyBookUid, Long moneyBookUserUid,
                                 UpdateMoneyBookMemberPermissionRequest request, Authentication authentication);

    void removeMember(Long moneyBookUid, Long moneyBookUserUid, Authentication authentication);
}
