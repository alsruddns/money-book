package com.moneybook.backend.closing.service;

import com.moneybook.backend.closing.dto.MonthClosingResponse;
import org.springframework.security.core.Authentication;

public interface MonthClosingService {
    MonthClosingResponse close(Long bookUid, int year, int month, Authentication authentication);
    MonthClosingResponse get(Long bookUid, int year, int month, Authentication authentication);
    void cancel(Long bookUid, int year, int month, Authentication authentication);
}
