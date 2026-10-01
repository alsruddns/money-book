package com.moneybook.backend.moneybook.controller;

import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.service.MoneyBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Access Token으로 인증된 사용자의 가계부 생성과 접근 가능한 가계부 목록 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/money-books")
@RequiredArgsConstructor
public class MoneyBookController {

    private final MoneyBookService moneyBookService;

    /**
     * 현재 사용자를 소유자와 전체 권한을 가진 수락 멤버로 등록하며 가계부를 생성한다.
     *
     * @param request 필수 가계부 이름(최대 100자)
     * @param authentication 검증된 Access Token의 인증 정보
     * @return 생성된 가계부 UID, 이름과 소유자 UID
     */
    @PostMapping
    public ResponseEntity<CreateMoneyBookResponse> create(
            @Valid @RequestBody CreateMoneyBookRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moneyBookService.create(request, authentication));
    }

    /**
     * 현재 사용자의 수락된 멤버십 중 읽기 권한이 있는 가계부를 최신 생성 순으로 조회한다.
     * 결과가 없으면 빈 배열을 반환한다.
     *
     * @param authentication 검증된 Access Token의 인증 정보
     * @return 접근 가능한 가계부와 각 멤버십의 권한
     */
    @GetMapping
    public ResponseEntity<List<MoneyBookListResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.list(authentication));
    }
}
