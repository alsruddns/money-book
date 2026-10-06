package com.moneybook.backend.moneybook.controller;

import com.moneybook.backend.moneybook.dto.MoneyBookSettingRequest;
import com.moneybook.backend.moneybook.dto.MoneyBookSettingResponse;
import com.moneybook.backend.moneybook.service.MoneyBookSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가계부별 달력 시작일 등 기본 설정 API를 제공한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/settings")
@RequiredArgsConstructor
public class MoneyBookSettingController {
    private final MoneyBookSettingService service;

    /** R 권한으로 가계부 설정을 조회한다. 값이 없으면 SUNDAY 기본값을 반환한다. */
    @GetMapping
    public MoneyBookSettingResponse get(@PathVariable Long moneyBookUid, Authentication authentication) {
        return service.get(moneyBookUid, authentication);
    }

    /** U 권한으로 가계부의 주 시작 요일을 변경한다. */
    @PutMapping
    public MoneyBookSettingResponse update(@PathVariable Long moneyBookUid,
            @Valid @RequestBody MoneyBookSettingRequest request, Authentication authentication) {
        return service.update(moneyBookUid, request, authentication);
    }
}
