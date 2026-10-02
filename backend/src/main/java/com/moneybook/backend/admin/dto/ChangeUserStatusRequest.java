package com.moneybook.backend.admin.dto;

import com.moneybook.backend.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(@NotNull UserStatus status) { }
