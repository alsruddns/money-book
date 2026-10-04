package com.moneybook.backend.recovery.dto;
import com.moneybook.backend.enums.SecurityQuestionCode;
public record AccountSecurityResponse(boolean securityQuestionConfigured,SecurityQuestionCode securityQuestionCode,
 int recoveryCodesRemaining,boolean emailVerified,String maskedEmail,boolean passwordChangeRequired) { }
