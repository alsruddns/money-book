package com.moneybook.backend.admin.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record AdminPageResponse<T>(List<T> content, int page, int size, long totalElements,
                                   int totalPages, boolean first, boolean last) {
    public static <T> AdminPageResponse<T> from(Page<T> p) {
        return new AdminPageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(),
                p.getTotalPages(), p.isFirst(), p.isLast());
    }
}
