package com.moneybook.backend.moneybook.dto;

public record MoneyBookMemberResponse(
        Long moneyBookUserUid,
        Long moneyBookUid,
        Long userUid,
        String nickname,
        boolean isOwner,
        boolean isAdmin,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete
) {
}
