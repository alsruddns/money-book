package com.moneybook.backend.activity.service;

import com.moneybook.backend.activity.dto.ActivityPageResponse;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;

public interface ActivityService {
    ActivityPageResponse search(Long bookUid, LocalDate startDate, LocalDate endDate, Long actorUid,
                                ActivityType activityType, ActivityTargetType targetType,
                                int page, int size, Authentication authentication);
}
