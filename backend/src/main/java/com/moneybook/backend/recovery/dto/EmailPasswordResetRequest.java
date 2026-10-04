package com.moneybook.backend.recovery.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record EmailPasswordResetRequest(@NotBlank String verificationToken,@NotBlank @Size(max=72) String newPassword,
 @NotBlank @Size(max=72) String newPasswordConfirm){
 @Override public String toString(){return "EmailPasswordResetRequest[verificationToken=REDACTED, passwords=REDACTED]";}
}
