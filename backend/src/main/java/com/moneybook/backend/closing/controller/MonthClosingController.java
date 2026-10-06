package com.moneybook.backend.closing.controller;

import com.moneybook.backend.closing.dto.MonthClosingResponse;
import com.moneybook.backend.closing.service.MonthClosingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가계부 월간 실적의 마감 snapshot을 생성·조회·취소한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/month-closings")
@RequiredArgsConstructor
public class MonthClosingController {
    private final MonthClosingService service;

    /** U 권한으로 현재 또는 과거 월의 실적과 예산을 한 번만 snapshot으로 저장한다. */
    @PostMapping("/{year}/{month}")
    public ResponseEntity<MonthClosingResponse> close(@PathVariable Long moneyBookUid, @PathVariable int year,
                                                       @PathVariable int month, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.close(moneyBookUid, year, month, authentication));
    }

    /** R 권한으로 과거에 저장한 마감 snapshot을 조회한다. */
    @GetMapping("/{year}/{month}")
    public MonthClosingResponse get(@PathVariable Long moneyBookUid, @PathVariable int year,
                                    @PathVariable int month, Authentication authentication) {
        return service.get(moneyBookUid, year, month, authentication);
    }

    /** U 권한으로 마감을 취소하고 해당 월의 거래·이체·예산 수정을 다시 허용한다. */
    @DeleteMapping("/{year}/{month}")
    public ResponseEntity<Void> cancel(@PathVariable Long moneyBookUid, @PathVariable int year,
                                       @PathVariable int month, Authentication authentication) {
        service.cancel(moneyBookUid, year, month, authentication);
        return ResponseEntity.noContent().build();
    }
}
