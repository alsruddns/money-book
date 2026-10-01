package com.moneybook.backend.category.service.impl;

import com.moneybook.backend.category.dto.CategoryResponse;
import com.moneybook.backend.category.dto.CreateCategoryRequest;
import com.moneybook.backend.category.dto.UpdateCategoryRequest;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.category.service.CategoryService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.MoneyBookPermission;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categories;
    private final MoneyBookPermissionProvider permissions;

    @Override
    @Transactional
    public CategoryResponse create(Long bookUid, CreateCategoryRequest request, Authentication authentication) {
        MoneyBook book = permissions.require(bookUid, authentication, MoneyBookPermission.CREATE);
        if (categories.existsByName(bookUid, request.transactionType(), request.name(), null)) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        try {
            return CategoryResponse.from(categories.save(MoneyBookCategory.create(
                    book, request.name(), request.transactionType(), request.sortOrder())));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long bookUid, TransactionType type, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.READ);
        return categories.findByMoneyBookUid(bookUid, type).stream().map(CategoryResponse::from).toList();
    }

    @Override
    @Transactional
    public CategoryResponse update(Long bookUid, Long categoryUid, UpdateCategoryRequest request,
                                   Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.UPDATE);
        MoneyBookCategory category = category(bookUid, categoryUid);
        if (categories.existsByName(bookUid, category.getTransactionType(), request.name(), categoryUid)) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        category.change(request.name(), request.sortOrder());
        try {
            categories.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public void delete(Long bookUid, Long categoryUid, Authentication authentication) {
        permissions.require(bookUid, authentication, MoneyBookPermission.DELETE);
        MoneyBookCategory category = category(bookUid, categoryUid);
        if (categories.isInUse(categoryUid)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
        try {
            categories.delete(category);
            categories.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
    }

    private MoneyBookCategory category(Long bookUid, Long categoryUid) {
        return categories.findByIdAndMoneyBookUid(categoryUid, bookUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
