package com.moneybook.backend.recovery.controller;

import com.moneybook.backend.recovery.*;
import com.moneybook.backend.recovery.dto.*;
import com.moneybook.backend.recovery.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 이메일 인증과 비로그인 비밀번호 복구 API를 제공한다. */
@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class PasswordRecoveryController {
 private final PasswordRecoveryService service;
 /** 개인정보가 아닌 고정 보안 질문 코드와 문구를 조회한다. */
 @GetMapping("/security-questions") public List<SecurityQuestionResponse> questions(){return service.questions();}
 /** 가입 또는 로그인 사용자의 새 이메일 인증 코드를 요청한다. PASSWORD_RESET은 별도 generic endpoint를 사용한다. */
 @PostMapping("/email-verifications/request") public EmailVerificationResponse request(@Valid @RequestBody EmailVerificationRequest r,Authentication a){return service.requestVerification(r.email(),r.purpose().name(),a);}
 /** 6자리 이메일 코드 검증 후 purpose에 맞는 10분 일회성 grant를 반환한다. */
 @PostMapping("/email-verifications/confirm") public EmailGrantResponse confirm(@Valid @RequestBody EmailVerificationConfirmRequest r){return service.confirmEmail(r.verificationUid(),r.code());}
 /** loginId와 인증 이메일이 일치하는 경우에만 메일을 발송하며 외부 응답은 항상 동일하다. */
 @PostMapping("/password-recovery/email/request") public EmailVerificationResponse requestReset(@Valid @RequestBody PasswordRecoveryRequest r){return service.requestPasswordResetEmail(r.loginId(),r.email());}
 /** 검증된 이메일 recovery grant를 소비해 비밀번호를 변경하고 모든 refresh session을 폐기한다. */
 @PostMapping("/password-recovery/email/reset") public ResponseEntity<Void> resetEmail(@Valid @RequestBody EmailRecoveryPasswordResetRequest r){service.resetByEmail(r.verificationToken(),r.newPassword(),r.newPasswordConfirm());return ResponseEntity.noContent().build();}
 /** loginId, question code, 정확히 일치하는 답변을 검증해 비밀번호를 변경한다. */
 @PostMapping("/password-recovery/security-question/reset") public ResponseEntity<Void> resetQuestion(@Valid @RequestBody QuestionPasswordResetRequest r){service.resetByQuestion(r.loginId(),r.questionCode().name(),r.answer(),r.newPassword(),r.newPasswordConfirm());return ResponseEntity.noContent().build();}
 /** 미사용 개인 복구코드 1개를 소비하고 비밀번호를 변경한다. */
 @PostMapping("/password-recovery/recovery-code/reset") public ResponseEntity<Void> resetCode(@Valid @RequestBody RecoveryCodePasswordResetRequest r){service.resetByRecoveryCode(r.loginId(),r.recoveryCode(),r.newPassword(),r.newPasswordConfirm());return ResponseEntity.noContent().build();}
}
