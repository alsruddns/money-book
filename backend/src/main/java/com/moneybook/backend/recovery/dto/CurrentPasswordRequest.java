package com.moneybook.backend.recovery.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CurrentPasswordRequest(@NotBlank @Size(max=72) String currentPassword){
 @Override public String toString(){return "CurrentPasswordRequest[currentPassword=REDACTED]";}
}
