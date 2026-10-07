package com.moneybook.backend.transfer.controller;

import com.moneybook.backend.transfer.dto.CreateTransferRequest;
import com.moneybook.backend.transfer.dto.TransferResponse;
import com.moneybook.backend.transfer.dto.UpdateTransferRequest;
import com.moneybook.backend.transfer.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

/** 가계부 계좌 간 이체를 관리하며 작업별 C/R/U/D 권한을 적용한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService service;

    /** C 권한으로 서로 다른 같은 가계부 계좌 간 이체를 등록한다. */
    @PostMapping
    public ResponseEntity<TransferResponse> create(@PathVariable Long moneyBookUid,
                                                   @Valid @RequestBody CreateTransferRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(moneyBookUid, request, authentication));
    }

    /** R 권한으로 지정 월의 이체를 날짜와 UID 역순으로 조회한다. */
    @GetMapping
    public ResponseEntity<List<TransferResponse>> list(@PathVariable Long moneyBookUid,
                                                        @RequestParam @Min(1) @Max(9999) int year,
                                                        @RequestParam @Min(1) @Max(12) int month,
                                                        Authentication authentication) {
        return ResponseEntity.ok(service.list(moneyBookUid, year, month, authentication));
    }

    /** R 권한으로 출금·입금 계좌 이름을 포함한 이체 상세를 조회한다. */
    @GetMapping("/{transferUid}")
    public ResponseEntity<TransferResponse> detail(@PathVariable Long moneyBookUid,
                                                   @PathVariable Long transferUid,
                                                   Authentication authentication) {
        return ResponseEntity.ok(service.detail(moneyBookUid, transferUid, authentication));
    }

    /** U 권한으로 이체를 수정하며 계좌 소속과 금액을 다시 검증한다. */
    @PatchMapping("/{transferUid}")
    public ResponseEntity<TransferResponse> update(@PathVariable Long moneyBookUid,
                                                   @PathVariable Long transferUid,
                                                   @Valid @RequestBody UpdateTransferRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(service.update(moneyBookUid, transferUid, request, authentication));
    }

    /** D 권한으로 지정 이체만 삭제하며 성공 시 204를 반환한다. */
    @DeleteMapping("/{transferUid}")
    public ResponseEntity<Void> delete(@PathVariable Long moneyBookUid, @PathVariable Long transferUid,
                                       Authentication authentication) {
        service.delete(moneyBookUid, transferUid, authentication);
        return ResponseEntity.noContent().build();
    }
}
