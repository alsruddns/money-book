package com.moneybook.backend.recovery.dto;
public record ApplyEmailGrantRequest(@jakarta.validation.constraints.NotBlank String verificationToken){
 @Override public String toString(){return "ApplyEmailGrantRequest[verificationToken=REDACTED]";}
}
