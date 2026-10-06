package com.moneybook.backend.auth.dto;

import java.util.List;
public record SignUpResDto(Long userUid, String nickname, List<String> recoveryCodes) {
    public SignUpResDto(Long uid,String nickname){this(uid,nickname,List.of());}
    @Override public String toString(){return "SignUpResDto[userUid="+userUid+", nickname=REDACTED, recoveryCodes=REDACTED]";}
}
