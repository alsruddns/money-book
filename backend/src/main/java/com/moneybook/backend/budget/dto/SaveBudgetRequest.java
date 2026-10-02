package com.moneybook.backend.budget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SaveBudgetRequest(
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal totalBudget,
        @NotNull List<@Valid CategoryBudgetRequest> categories
) { }
