package com.moneybook.backend.admin.controller;

import com.moneybook.backend.activity.dto.ActivityPageResponse;
import com.moneybook.backend.activity.service.ActivityService;
import com.moneybook.backend.admin.dto.AdminActivityResponse;
import com.moneybook.backend.admin.dto.AdminPageResponse;
import com.moneybook.backend.admin.service.AdminService;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/** 서비스 관리자가 MoneyBook 전체 Activity timeline을 검색하는 API. */
@RestController
@RequestMapping("/admin/activities")
@RequiredArgsConstructor
public class AdminActivityController {
    private final AdminService service;

    /** 가계부·actor·유형·대상·기간 필터를 적용해 전체 Activity를 최신순 조회한다. */
    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminActivityResponse>> search(
            @RequestParam(required=false) Long moneyBookUid, @RequestParam(required=false) Long actorUserUid,
            @RequestParam(required=false) ActivityType activityType,
            @RequestParam(required=false) ActivityTargetType targetType,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(service.activities(moneyBookUid,actorUserUid,activityType,targetType,
                startDate,endDate,page,size,authentication));
    }
}
