package com.moneybook.backend.moneybook.dto;

import com.moneybook.backend.enums.InvitationStatus;

public record PendingInvitationResponse(
        Long moneyBookUserUid,
        Long moneyBookUid,
        String moneyBookName,
        Long ownerUserUid,
        InvitationStatus invitationStatus,
        boolean isAdmin,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete
) {
}
