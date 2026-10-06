package com.moneybook.backend.budget.controller;

import com.moneybook.backend.budget.dto.MonthlyBudgetResponse;
import com.moneybook.backend.budget.dto.SaveBudgetRequest;
import com.moneybook.backend.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가계부의 월별 총예산과 지출 카테고리 예산 API를 제공한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    /**
     * R 권한으로 해당 월 예산과 실제 EXPENSE 집계를 조회한다. 예산 미설정 시 configured=false를 반환한다.
     */
    @GetMapping("/{year}/{month}")
    public MonthlyBudgetResponse get(@PathVariable Long moneyBookUid, @PathVariable int year,
                                     @PathVariable int month, Authentication authentication) {
        return budgetService.get(moneyBookUid, year, month, authentication);
    }

    /**
     * U 권한으로 월 예산을 생성하거나 수정한다. 카테고리 목록은 해당 월 예산의 최종 목록이다.
     */
    @PutMapping("/{year}/{month}")
    public MonthlyBudgetResponse save(@PathVariable Long moneyBookUid, @PathVariable int year,
                                      @PathVariable int month, @Valid @RequestBody SaveBudgetRequest request,
                                      Authentication authentication) {
        return budgetService.save(moneyBookUid, year, month, request, authentication);
    }
}
