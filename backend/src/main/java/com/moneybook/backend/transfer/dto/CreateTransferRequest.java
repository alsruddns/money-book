package com.moneybook.backend.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransferRequest(
        @NotNull Long fromAccountUid,
        @NotNull Long toAccountUid,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull LocalDate transferDate,
        @Size(max = 500) String memo
) { }
