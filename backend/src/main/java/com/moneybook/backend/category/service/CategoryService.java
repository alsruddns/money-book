package com.moneybook.backend.category.service;

import com.moneybook.backend.category.dto.CategoryResponse;
import com.moneybook.backend.category.dto.CreateCategoryRequest;
import com.moneybook.backend.category.dto.UpdateCategoryRequest;
import com.moneybook.backend.enums.TransactionType;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(Long moneyBookUid, CreateCategoryRequest request, Authentication authentication);
    List<CategoryResponse> list(Long moneyBookUid, TransactionType type, Authentication authentication);
    CategoryResponse update(Long moneyBookUid, Long categoryUid, UpdateCategoryRequest request,
                            Authentication authentication);
    void delete(Long moneyBookUid, Long categoryUid, Authentication authentication);
}
