package com.moneybook.backend.recovery.controller;
import com.moneybook.backend.recovery.dto.*;
import com.moneybook.backend.recovery.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
/** 로그인 사용자의 비밀 복구 수단과 인증 이메일을 관리하는 API. */
@RestController @RequestMapping("/account/security") @RequiredArgsConstructor
public class AccountSecurityController {
 private final PasswordRecoveryService service;
 /** 설정 여부, 질문 코드, 남은 코드 수와 마스킹 이메일만 조회한다. */
 @GetMapping public AccountSecurityResponse get(Authentication a){return service.accountSecurity(a);}
 /** 현재 비밀번호 재확인 후 보안 질문을 생성하거나 변경한다. */
 @PatchMapping("/security-question") public ResponseEntity<Void> question(Authentication a,@Valid @RequestBody SecurityQuestionUpdateRequest r){service.updateQuestion(a,r);return ResponseEntity.noContent().build();}
 /** 현재 비밀번호 확인 후 기존 코드를 폐기하고 8개 새 원문 코드를 한 번만 반환한다. */
 @PostMapping("/recovery-codes/regenerate") public RecoveryCodesResponse regenerate(Authentication a,@Valid @RequestBody CurrentPasswordRequest r){return service.regenerateCodes(a,r.currentPassword());}
 /** 인증 완료 grant를 사용해 현재 계정의 이메일을 등록하거나 변경한다. */
 @PutMapping("/email") public ResponseEntity<Void> email(Authentication a,@Valid @RequestBody ApplyEmailGrantRequest r){service.applyEmail(a,r.verificationToken());return ResponseEntity.noContent().build();}
 /** 현재 비밀번호 재확인 후 인증 이메일을 삭제한다. */
 @DeleteMapping("/email") public ResponseEntity<Void> removeEmail(Authentication a,@Valid @RequestBody CurrentPasswordRequest r){service.removeEmail(a,r.currentPassword());return ResponseEntity.noContent().build();}
}
