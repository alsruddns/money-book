package com.moneybook.backend.activity.service.impl;

import com.moneybook.backend.activity.dto.ActivityPageResponse;
import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.activity.service.ActivityService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private final ActivityRepository activities;
    private final MoneyBookPermissionProvider permissions;

    /** Reads a bounded page of activity snapshots after verifying MoneyBook read permission. */
    @Override @Transactional(readOnly = true)
    public ActivityPageResponse search(Long bookUid, LocalDate start, LocalDate end, Long actor,
                                      ActivityType type, ActivityTargetType target, int page, int size,
                                      Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        if ((start != null && end != null && start.isAfter(end)) || page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        var result = activities.search(bookUid, start, end, actor, type, target,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("activityUid"))));
        return ActivityPageResponse.from(result.map(com.moneybook.backend.activity.dto.ActivityResponse::from));
    }
}
