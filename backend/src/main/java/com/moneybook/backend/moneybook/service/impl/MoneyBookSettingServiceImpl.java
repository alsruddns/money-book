package com.moneybook.backend.moneybook.service.impl;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBookSetting;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.WeekStartDay;
import com.moneybook.backend.moneybook.dto.MoneyBookSettingRequest;
import com.moneybook.backend.moneybook.dto.MoneyBookSettingResponse;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
import com.moneybook.backend.moneybook.service.MoneyBookSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoneyBookSettingServiceImpl implements MoneyBookSettingService {
    private final MoneyBookPermissionProvider permissions;
    private final MoneyBookSettingRepository settings;

    @Override
    @Transactional(readOnly = true)
    public MoneyBookSettingResponse get(Long bookUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        WeekStartDay day = settings.findByMoneyBookUid(bookUid).map(MoneyBookSetting::getWeekStartDay)
                .orElse(WeekStartDay.SUNDAY);
        return new MoneyBookSettingResponse(bookUid, day);
    }

    @Override
    @Transactional
    public MoneyBookSettingResponse update(Long bookUid, MoneyBookSettingRequest request,
                                          Authentication authentication) {
        var book = permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        if (request == null || request.weekStartDay() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        MoneyBookSetting setting = settings.findByMoneyBookUid(bookUid)
                .orElseGet(() -> MoneyBookSetting.create(book, WeekStartDay.SUNDAY));
        setting.changeWeekStartDay(request.weekStartDay());
        return new MoneyBookSettingResponse(bookUid, settings.save(setting).getWeekStartDay());
    }
}
