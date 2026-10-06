package com.moneybook.backend.moneybook.dto;

import jakarta.validation.constraints.NotNull;

public record TransferMoneyBookOwnerRequest(@NotNull Long targetUserUid) { }
