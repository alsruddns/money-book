package com.moneybook.backend.recovery.dto;
import com.moneybook.backend.enums.SecurityQuestionCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record QuestionPasswordResetRequest(@NotBlank @Size(max=100) String loginId,@NotNull SecurityQuestionCode questionCode,
 @NotBlank @Size(max=128) String answer,@NotBlank @Size(max=72) String newPassword,@NotBlank @Size(max=72) String newPasswordConfirm){
 @Override public String toString(){return "QuestionPasswordResetRequest[loginId=REDACTED, answer=REDACTED, passwords=REDACTED]";}
}
