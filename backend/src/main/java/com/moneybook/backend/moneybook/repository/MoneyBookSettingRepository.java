package com.moneybook.backend.moneybook.repository;

import com.moneybook.backend.entity.MoneyBookSetting;
import java.util.Optional;

public interface MoneyBookSettingRepository {
    Optional<MoneyBookSetting> findByMoneyBookUid(Long moneyBookUid);
    MoneyBookSetting save(MoneyBookSetting setting);
}
