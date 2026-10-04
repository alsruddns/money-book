package com.moneybook.backend.recovery.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RecoveryCodePasswordResetRequest(@NotBlank @Size(max=100) String loginId,@NotBlank String recoveryCode,
 @NotBlank @Size(max=72) String newPassword,@NotBlank @Size(max=72) String newPasswordConfirm){
 @Override public String toString(){return "RecoveryCodePasswordResetRequest[loginId=REDACTED, recoveryCode=REDACTED, passwords=REDACTED]";}
}
