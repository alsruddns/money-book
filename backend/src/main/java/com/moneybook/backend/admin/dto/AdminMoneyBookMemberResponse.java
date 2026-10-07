package com.moneybook.backend.admin.dto;

import com.moneybook.backend.enums.InvitationStatus;
import java.time.LocalDateTime;

public record AdminMoneyBookMemberResponse(Long userUid, String nickname, boolean isOwner,
        boolean isAdmin, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete,
        InvitationStatus membershipStatus, LocalDateTime joinedAt) { }
