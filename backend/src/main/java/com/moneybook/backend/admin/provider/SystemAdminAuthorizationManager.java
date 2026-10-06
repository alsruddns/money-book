package com.moneybook.backend.admin.provider;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.core.Authentication;
import java.util.function.Supplier;

/** /admin 요청마다 DB의 현재 역할과 ACTIVE 상태를 확인한다. */
public class SystemAdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final SystemAdminAuthorizationProvider provider;

    public SystemAdminAuthorizationManager(SystemAdminAuthorizationProvider provider) {
        this.provider = provider;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         RequestAuthorizationContext context) {
        return new AuthorizationDecision(provider != null && provider.hasAdminAccess(authentication.get()));
    }
}
