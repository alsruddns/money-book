package com.moneybook.backend.account.dto;

import com.moneybook.backend.enums.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType accountType,
        @NotNull @Min(0) Integer sortOrder
) { }
