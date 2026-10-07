package com.moneybook.backend.calendar.dto;

import java.math.BigDecimal;

public record CalendarTransferResponse(Long transferUid, Long fromAccountUid, String fromAccountName,
                                       Long toAccountUid, String toAccountName, BigDecimal amount, String memo) { }
