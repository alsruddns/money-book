package com.moneybook.backend.recurring.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRecurringTransactionActiveRequest(@NotNull Boolean active) { }
