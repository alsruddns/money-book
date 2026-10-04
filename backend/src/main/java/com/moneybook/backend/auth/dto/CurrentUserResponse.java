package com.moneybook.backend.auth.dto;

import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.enums.SystemRole;

public record CurrentUserResponse(Long userUid, String nickname, UserStatus status, SystemRole systemRole,
                                  boolean passwordChangeRequired) {
    public CurrentUserResponse(Long uid,String nickname,UserStatus status,SystemRole role){this(uid,nickname,status,role,false);}
}
