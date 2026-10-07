package com.moneybook.backend.category.dto;

import com.moneybook.backend.enums.TransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull TransactionType transactionType,
        @NotNull @Min(0) Integer sortOrder
) { }
