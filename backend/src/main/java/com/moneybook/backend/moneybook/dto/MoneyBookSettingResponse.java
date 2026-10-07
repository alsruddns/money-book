package com.moneybook.backend.moneybook.dto;

import com.moneybook.backend.enums.WeekStartDay;

public record MoneyBookSettingResponse(Long moneyBookUid, WeekStartDay weekStartDay) { }
