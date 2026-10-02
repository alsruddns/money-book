package com.moneybook.backend.activity.repository;

import com.moneybook.backend.entity.MoneyBookActivity;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface ActivityRepository {
    MoneyBookActivity save(MoneyBookActivity activity);
    /** bookUid가 null이면 관리자 전체 활동 검색이며, 그 외에는 단일 가계부 범위다. */
    Page<MoneyBookActivity> search(Long bookUid, LocalDate startDate, LocalDate endDate, Long actorUid,
                                   ActivityType activityType, ActivityTargetType targetType, Pageable pageable);
}
