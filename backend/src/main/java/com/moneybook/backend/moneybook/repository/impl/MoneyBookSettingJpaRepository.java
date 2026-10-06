package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBookSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MoneyBookSettingJpaRepository extends JpaRepository<MoneyBookSetting, Long> {
    Optional<MoneyBookSetting> findByMoneyBook_MoneyBookUid(Long moneyBookUid);
}
