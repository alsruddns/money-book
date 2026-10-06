package com.moneybook.backend.backup.controller;

import com.moneybook.backend.backup.dto.BackupRestoreResponse;
import com.moneybook.backend.backup.dto.BackupValidationResponse;
import com.moneybook.backend.backup.service.MoneyBookBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** MoneyBook 업무 데이터의 JSON 백업, 무변경 검증, 새 가계부 복원을 제공한다. */
@RestController
@RequestMapping("/money-books")
@RequiredArgsConstructor
public class MoneyBookBackupController {
    private final MoneyBookBackupService service;

    /** owner 또는 ACCEPTED 관리자만 전체 업무 백업 파일을 다운로드할 수 있다. */
    @GetMapping("/{moneyBookUid}/backups/export")
    public ResponseEntity<StreamingResponseBody> export(@PathVariable Long moneyBookUid, Authentication authentication) {
        StreamingResponseBody body = output -> service.writeBackup(moneyBookUid, authentication, output);
        String filename = "money-book-" + moneyBookUid + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(body);
    }

    /** 최대 20MB JSON을 DB 변경 없이 검사하고 건수 및 거래 기간 미리보기를 반환한다. */
    @PostMapping(value = "/backups/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BackupValidationResponse> validate(@RequestPart("file") MultipartFile file,
                                                              Authentication authentication) {
        return ResponseEntity.ok(service.validate(file, authentication));
    }

    /** 유효한 백업을 현재 사용자가 Owner인 새 MoneyBook으로 원자적으로 복원한다. */
    @PostMapping(value = "/backups/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BackupRestoreResponse> restore(@RequestPart("file") MultipartFile file,
                                                          Authentication authentication) {
        return ResponseEntity.status(201).body(service.restore(file, authentication));
    }
}
