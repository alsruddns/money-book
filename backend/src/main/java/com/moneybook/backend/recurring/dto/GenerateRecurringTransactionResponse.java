package com.moneybook.backend.recurring.dto;

import java.time.LocalDate;

public record GenerateRecurringTransactionResponse(LocalDate baseDate, int generatedCount) { }
