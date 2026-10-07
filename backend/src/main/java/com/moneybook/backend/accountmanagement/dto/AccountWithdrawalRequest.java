package com.moneybook.backend.accountmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Password is used only for re-authentication and is never included in the generated record string. */
public record AccountWithdrawalRequest(@NotBlank @Size(max = 72) String currentPassword) {
    @Override
    public String toString() {
        return "AccountWithdrawalRequest[currentPassword=REDACTED]";
    }
}
