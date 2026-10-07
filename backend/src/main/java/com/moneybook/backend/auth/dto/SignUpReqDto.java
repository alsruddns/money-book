package com.moneybook.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.moneybook.backend.enums.SecurityQuestionCode;
import jakarta.validation.constraints.NotNull;
import com.moneybook.backend.common.validation.NoSurroundingWhitespace;

public record SignUpReqDto(
        @NotBlank @Size(max = 100) String loginId,
        @NotBlank @Size(max = 72) String password,
        @NotBlank @Size(max = 72) String passwordConfirm,
        @NotBlank @Size(max = 50) String nickname,
        @NotNull SecurityQuestionCode securityQuestionCode,
        @NotBlank @Size(max = 128) @NoSurroundingWhitespace String securityAnswer,
        String emailVerificationToken
) {
    /** Test and internal source compatibility; JSON requests still require explicit recovery settings. */
    public SignUpReqDto(String loginId,String password,String passwordConfirm,String nickname) {
        this(loginId,password,passwordConfirm,nickname,null,null,null);
    }
    @Override
    public String toString() {
        return "SignUpReqDto[loginId=REDACTED, passwords=REDACTED, securityAnswer=REDACTED, emailVerificationToken=REDACTED]";
    }
}
