package com.moneybook.backend.activity.controller;

import com.moneybook.backend.activity.dto.ActivityPageResponse;
import com.moneybook.backend.activity.service.ActivityService;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/** 가계부 변경 활동을 읽기 권한으로 페이지 조회하는 API. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService service;

    /** 날짜, actor, 활동 및 대상 조건으로 최신 활동을 조회한다. 기본 20건, 최대 100건이다. */
    @GetMapping
    public ResponseEntity<ActivityPageResponse> search(@PathVariable Long moneyBookUid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long actorUserUid,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) ActivityTargetType targetType,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(service.search(moneyBookUid, startDate, endDate, actorUserUid,
                activityType, targetType, page, size, authentication));
    }
}
