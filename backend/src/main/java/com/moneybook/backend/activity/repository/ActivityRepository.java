package com.moneybook.backend.activity.repository;

import com.moneybook.backend.entity.MoneyBookActivity;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface ActivityRepository {
    MoneyBookActivity save(MoneyBookActivity activity);
    Page<MoneyBookActivity> search(Long bookUid, LocalDate startDate, LocalDate endDate, Long actorUid,
                                   ActivityType activityType, ActivityTargetType targetType, Pageable pageable);
}
