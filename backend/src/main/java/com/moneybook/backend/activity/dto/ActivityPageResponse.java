package com.moneybook.backend.activity.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record ActivityPageResponse(List<ActivityResponse> content, int page, int size, long totalElements,
                                   int totalPages, boolean first, boolean last) {
    public static ActivityPageResponse from(Page<ActivityResponse> page) {
        return new ActivityPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
