package com.moneybook.backend.transaction.dto;

import java.util.List;

public record TransactionSearchResponse(List<TransactionResponse> content, int page, int size,
                                        long totalElements, long totalPages, boolean first, boolean last) { }
