package com.moneybook.backend.moneybook.repository;

/** One accepted member with the user name and book owner loaded in a single query. */
public record MoneyBookMemberRow(
        Long moneyBookUserUid,
        Long moneyBookUid,
        Long userUid,
        String nickname,
        Long ownerUserUid,
        boolean isAdmin,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete
) {
}
