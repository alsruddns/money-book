package com.moneybook.backend.admin.dto;

import com.moneybook.backend.enums.SystemRole;

public record AdminMeResponse(Long userUid, String nickname, SystemRole systemRole) { }
