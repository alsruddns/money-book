package com.moneybook.backend.moneybook.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMoneyBookMemberPermissionRequest(
        @NotNull Boolean isAdmin,
        @NotNull Boolean canCreate,
        @NotNull Boolean canRead,
        @NotNull Boolean canUpdate,
        @NotNull Boolean canDelete
) {
}
