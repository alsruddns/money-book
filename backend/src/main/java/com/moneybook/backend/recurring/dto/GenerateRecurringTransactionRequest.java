package com.moneybook.backend.recurring.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateRecurringTransactionRequest(@NotNull LocalDate baseDate) { }
