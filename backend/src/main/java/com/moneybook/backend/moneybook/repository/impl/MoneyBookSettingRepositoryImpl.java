package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBookSetting;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MoneyBookSettingRepositoryImpl implements MoneyBookSettingRepository {
    private final MoneyBookSettingJpaRepository jpa;
    @Override public Optional<MoneyBookSetting> findByMoneyBookUid(Long uid) {
        return jpa.findByMoneyBook_MoneyBookUid(uid);
    }
    @Override public MoneyBookSetting save(MoneyBookSetting setting) { return jpa.saveAndFlush(setting); }
}
