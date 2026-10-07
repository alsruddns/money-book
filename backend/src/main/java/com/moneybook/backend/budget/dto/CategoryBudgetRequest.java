package com.moneybook.backend.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CategoryBudgetRequest(
        @NotNull Long categoryUid,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal amount
) { }
