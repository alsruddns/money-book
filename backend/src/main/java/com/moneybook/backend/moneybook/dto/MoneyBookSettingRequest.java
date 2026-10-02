package com.moneybook.backend.moneybook.dto;

import com.moneybook.backend.enums.WeekStartDay;
import jakarta.validation.constraints.NotNull;

public record MoneyBookSettingRequest(@NotNull WeekStartDay weekStartDay) { }
