package com.moneybook.backend.category.controller;

import com.moneybook.backend.category.dto.CategoryResponse;
import com.moneybook.backend.category.dto.CreateCategoryRequest;
import com.moneybook.backend.category.dto.UpdateCategoryRequest;
import com.moneybook.backend.category.service.CategoryService;
import com.moneybook.backend.enums.TransactionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 가계부별 수입·지출 카테고리 API를 제공한다. 모든 요청에 가계부 권한을 적용한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    /** C 권한으로 유형과 정렬 순서를 지정해 카테고리를 등록한다. */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@PathVariable Long moneyBookUid,
                                                    @Valid @RequestBody CreateCategoryRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(moneyBookUid, request, authentication));
    }

    /** R 권한으로 카테고리를 조회하며 유형 필터를 생략하면 전체를 반환한다. */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@PathVariable Long moneyBookUid,
                                                        @RequestParam(required = false) TransactionType transactionType,
                                                        Authentication authentication) {
        return ResponseEntity.ok(service.list(moneyBookUid, transactionType, authentication));
    }

    /** U 권한으로 이름과 정렬 순서를 변경한다. 거래 유형은 유지한다. */
    @PatchMapping("/{categoryUid}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long moneyBookUid, @PathVariable Long categoryUid,
                                                    @Valid @RequestBody UpdateCategoryRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(service.update(moneyBookUid, categoryUid, request, authentication));
    }

    /** D 권한으로 사용되지 않은 카테고리만 삭제한다. */
    @DeleteMapping("/{categoryUid}")
    public ResponseEntity<Void> delete(@PathVariable Long moneyBookUid, @PathVariable Long categoryUid,
                                       Authentication authentication) {
        service.delete(moneyBookUid, categoryUid, authentication);
        return ResponseEntity.noContent().build();
    }
}
