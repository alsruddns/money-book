package com.moneybook.backend.moneybook.dto;

import com.moneybook.backend.enums.InvitationStatus;

public record InvitationResponse(
        Long moneyBookUserUid,
        Long moneyBookUid,
        Long userUid,
        InvitationStatus invitationStatus,
        boolean isAdmin,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete
) {
}
