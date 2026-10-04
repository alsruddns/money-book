package com.moneybook.backend.recovery.dto;
import com.moneybook.backend.enums.SecurityQuestionCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record SecurityQuestionUpdateRequest(@NotNull SecurityQuestionCode questionCode,@NotBlank @Size(max=128) String answer,
 @NotBlank @Size(max=72) String currentPassword){
 @Override public String toString(){return "SecurityQuestionUpdateRequest[questionCode="+questionCode+", answer=REDACTED, currentPassword=REDACTED]";}
}
