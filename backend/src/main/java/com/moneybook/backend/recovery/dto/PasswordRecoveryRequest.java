package com.moneybook.backend.recovery.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
public record PasswordRecoveryRequest(@NotBlank @Size(max=100) String loginId,@NotBlank @Email @Size(max=254) String email) {
 @Override public String toString(){return "PasswordRecoveryRequest[credentials=REDACTED]";}
}
