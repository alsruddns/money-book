package com.moneybook.backend.accountmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountProfileUpdateReqDto(
        @NotBlank @Size(max = 50) String nickname
) {
}
