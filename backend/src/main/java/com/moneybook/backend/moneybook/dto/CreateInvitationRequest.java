package com.moneybook.backend.moneybook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInvitationRequest(
        @NotBlank @Size(max = 100) String loginId,
        @NotNull Boolean isAdmin,
        @NotNull Boolean canCreate,
        @NotNull Boolean canRead,
        @NotNull Boolean canUpdate,
        @NotNull Boolean canDelete
) {
}
