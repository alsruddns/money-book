package com.moneybook.backend.recovery.dto;
public record EmailGrantResponse(@jakarta.validation.constraints.NotBlank String verificationToken) {
 @Override public String toString(){return "EmailGrantResponse[verificationToken=REDACTED]";}
}
