package com.moneybook.backend.moneybook.dto;

public record MoneyBookListResponse(
        Long moneyBookUid,
        String name,
        Long ownerUserUid,
        boolean isOwner,
        boolean isAdmin,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete
) {
}
