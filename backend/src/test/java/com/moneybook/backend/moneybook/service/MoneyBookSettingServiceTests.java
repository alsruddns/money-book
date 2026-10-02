package com.moneybook.backend.moneybook.service;

import com.moneybook.backend.activity.ActivityRecorder;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookSetting;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.WeekStartDay;
import com.moneybook.backend.moneybook.dto.MoneyBookSettingRequest;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
import com.moneybook.backend.moneybook.service.impl.MoneyBookSettingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoneyBookSettingServiceTests {
    private final MoneyBookPermissionProvider permissions=mock(MoneyBookPermissionProvider.class);
    private final MoneyBookSettingRepository settings=mock(MoneyBookSettingRepository.class);
    private final MoneyBookSettingServiceImpl service=new MoneyBookSettingServiceImpl(permissions,settings,mock(ActivityRecorder.class));
    private final Authentication authentication=mock(Authentication.class);

    @Test void missingSettingReadsAsSundayWithReadPermission() {
        when(permissions.require(11L,authentication,MoneyBookPermission.READ)).thenReturn(MoneyBook.create("home",4L));
        when(settings.findByMoneyBookUid(11L)).thenReturn(Optional.empty());
        assertEquals(WeekStartDay.SUNDAY,service.get(11L,authentication).weekStartDay());
        verify(permissions).require(11L,authentication,MoneyBookPermission.READ);
    }

    @Test void updatesMondayWithUpdatePermission() {
        MoneyBook book=MoneyBook.create("home",4L);
        when(permissions.require(11L,authentication,MoneyBookPermission.UPDATE)).thenReturn(book);
        when(settings.findByMoneyBookUid(11L)).thenReturn(Optional.empty());
        when(settings.save(any())).thenAnswer(inv->inv.getArgument(0));
        assertEquals(WeekStartDay.MONDAY,service.update(11L,new MoneyBookSettingRequest(WeekStartDay.MONDAY),authentication).weekStartDay());
        verify(permissions).require(11L,authentication,MoneyBookPermission.UPDATE);
    }

    @Test void deniedUpdateDoesNotSaveSetting() {
        when(permissions.require(11L,authentication,MoneyBookPermission.UPDATE)).thenThrow(new BusinessException(ErrorCode.MONEY_BOOK_UPDATE_FORBIDDEN));
        assertThrows(BusinessException.class,()->service.update(11L,new MoneyBookSettingRequest(WeekStartDay.MONDAY),authentication));
        verify(settings,never()).save(any());
    }
}
