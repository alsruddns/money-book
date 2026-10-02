package com.moneybook.backend.accountmanagement.service;

import com.moneybook.backend.accountmanagement.dto.RefreshSessionResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AccountSessionService {
    List<RefreshSessionResponse> list(Authentication authentication);
    void revoke(Authentication authentication, Long sessionUid);
    void revokeCurrent(Authentication authentication);
    void revokeAll(Authentication authentication);
}
