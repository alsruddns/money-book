package com.moneybook.backend.activity.repository.impl;

import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.entity.MoneyBookActivity;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepository {
    private final ActivityJpaRepository jpa;
    @Override public MoneyBookActivity save(MoneyBookActivity activity) { return jpa.save(activity); }
    @Override public Page<MoneyBookActivity> search(Long bookUid, LocalDate start, LocalDate end, Long actor,
                                                    ActivityType type, ActivityTargetType target, Pageable pageable) {
        return jpa.search(bookUid, start == null ? null : start.atStartOfDay(),
                end == null ? null : end.plusDays(1).atStartOfDay(), actor, type, target, pageable);
    }
}
