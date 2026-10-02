package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.moneybook.dto.MoneyBookSettingRequest;
import com.moneybook.backend.moneybook.dto.MoneyBookSettingResponse;
import org.springframework.security.core.Authentication;

public interface MoneyBookSettingService {
    MoneyBookSettingResponse get(Long moneyBookUid, Authentication authentication);
    MoneyBookSettingResponse update(Long moneyBookUid, MoneyBookSettingRequest request,
                                    Authentication authentication);
}
