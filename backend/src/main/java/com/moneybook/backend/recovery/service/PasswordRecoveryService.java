package com.moneybook.backend.recovery.service;
import com.moneybook.backend.recovery.dto.*;
import org.springframework.security.core.Authentication;
import java.util.List;
public interface PasswordRecoveryService {
 List<SecurityQuestionResponse> questions();
 EmailVerificationResponse requestVerification(String email,String purpose,Authentication authentication);
 EmailVerificationResponse requestPasswordResetEmail(String loginId,String email);
 EmailGrantResponse confirmEmail(Long verificationUid,String code);
 void resetByEmail(String token,String password,String confirm);
 void resetByQuestion(String loginId,String code,String answer,String password,String confirm);
 void resetByRecoveryCode(String loginId,String recoveryCode,String password,String confirm);
 AccountSecurityResponse accountSecurity(Authentication authentication);
 void updateQuestion(Authentication authentication,SecurityQuestionUpdateRequest request);
 RecoveryCodesResponse regenerateCodes(Authentication authentication,String currentPassword);
 void applyEmail(Authentication authentication,String verificationToken);
 void removeEmail(Authentication authentication,String currentPassword);
}
