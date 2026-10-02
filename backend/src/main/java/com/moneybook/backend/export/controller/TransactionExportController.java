package com.moneybook.backend.export.controller;

import com.moneybook.backend.export.service.ExportService;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchSort;
import com.moneybook.backend.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** R 권한으로 날짜와 거래 필터에 맞는 파일 다운로드를 제공한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/exports")
@RequiredArgsConstructor
public class TransactionExportController {
    private final ExportService exports;

    /** UTF-8 BOM CSV를 내려받는다. 조회 조건은 거래 검색 API와 동일하며 날짜 양 끝을 포함한다. */
    @GetMapping(value = "/transactions.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> csv(@PathVariable Long moneyBookUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) Long categoryUid, @RequestParam(required = false) Long accountUid,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAmount, @RequestParam(required = false) BigDecimal maxAmount,
            Authentication authentication) {
        TransactionSearchRequest filters = filters(startDate, endDate, transactionType, categoryUid,
                accountUid, keyword, minAmount, maxAmount);
        exports.validate(moneyBookUid, filters, authentication);
        StreamingResponseBody body = output -> exports.writeCsv(moneyBookUid, filters, authentication, output);
        return file(moneyBookUid, "csv", "text/csv; charset=UTF-8", body);
    }

    /** SXSSF XLSX를 내려받는다. 대량 데이터는 keyset batch로 순회하고 금액은 numeric cell로 기록한다. */
    @GetMapping(value = "/transactions.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<StreamingResponseBody> xlsx(@PathVariable Long moneyBookUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) Long categoryUid, @RequestParam(required = false) Long accountUid,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAmount, @RequestParam(required = false) BigDecimal maxAmount,
            Authentication authentication) {
        TransactionSearchRequest filters = filters(startDate, endDate, transactionType, categoryUid,
                accountUid, keyword, minAmount, maxAmount);
        exports.validate(moneyBookUid, filters, authentication);
        StreamingResponseBody body = output -> exports.writeXlsx(moneyBookUid, filters, authentication, output);
        return file(moneyBookUid, "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", body);
    }

    private TransactionSearchRequest filters(LocalDate start, LocalDate end, TransactionType type, Long category,
                                              Long account, String keyword, BigDecimal min, BigDecimal max) {
        return new TransactionSearchRequest(start, end, type, category, account, keyword, min, max,
                0, 20, TransactionSearchSort.DATE_DESC);
    }

    private ResponseEntity<StreamingResponseBody> file(Long bookUid, String extension, String contentType,
                                                       StreamingResponseBody body) {
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "money-book-" + bookUid + "-" + timestamp + "." + extension;
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(body);
    }
}
