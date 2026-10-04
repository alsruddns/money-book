package com.moneybook.backend.recovery.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
public record EmailVerificationConfirmRequest(@NotNull Long verificationUid,@NotBlank @Pattern(regexp="[0-9]{6}") String code) {
 @Override public String toString(){return "EmailVerificationConfirmRequest[verificationUid="+verificationUid+", code=REDACTED]";}
}
