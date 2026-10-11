package com.moneybook.backend.category.dto;

import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.CategoryLevel;
import java.util.List;

public record CategoryTreeResponse(Long categoryUid, String name, CategoryLevel level,
                                   List<CategoryTreeResponse> children) {
    public static CategoryTreeResponse from(MoneyBookCategory category, List<CategoryTreeResponse> children) {
        return new CategoryTreeResponse(category.getCategoryUid(), category.getName(), category.getCategoryLevel(), children);
    }
}
