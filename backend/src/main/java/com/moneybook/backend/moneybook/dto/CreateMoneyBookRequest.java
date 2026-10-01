package com.moneybook.backend.moneybook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMoneyBookRequest(
        @NotBlank @Size(max = 100) String name
) {
}
