package com.moneybook.backend.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Environment-overridable limits for the single-instance in-memory limiter. */
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {
    private int loginPerMinute = 10;
    private int loginPer15Minutes = 30;
    private int loginIdPer5Minutes = 10;
    private int signupPerHour = 8;
    private int refreshPerMinute = 30;
    private int logoutPerMinute = 30;
    private int passwordChangePer10Minutes = 5;
    private int withdrawalPer10Minutes = 3;
    private int logoutAllPerMinute = 5;
    private int adminMutationPerMinute = 30;
    private int maximumKeys = 50000;

    public int getLoginPerMinute() { return loginPerMinute; }
    public void setLoginPerMinute(int value) { loginPerMinute = positive(value); }
    public int getLoginPer15Minutes() { return loginPer15Minutes; }
    public void setLoginPer15Minutes(int value) { loginPer15Minutes = positive(value); }
    public int getLoginIdPer5Minutes() { return loginIdPer5Minutes; }
    public void setLoginIdPer5Minutes(int value) { loginIdPer5Minutes = positive(value); }
    public int getSignupPerHour() { return signupPerHour; }
    public void setSignupPerHour(int value) { signupPerHour = positive(value); }
    public int getRefreshPerMinute() { return refreshPerMinute; }
    public void setRefreshPerMinute(int value) { refreshPerMinute = positive(value); }
    public int getLogoutPerMinute() { return logoutPerMinute; }
    public void setLogoutPerMinute(int value) { logoutPerMinute = positive(value); }
    public int getPasswordChangePer10Minutes() { return passwordChangePer10Minutes; }
    public void setPasswordChangePer10Minutes(int value) { passwordChangePer10Minutes = positive(value); }
    public int getWithdrawalPer10Minutes() { return withdrawalPer10Minutes; }
    public void setWithdrawalPer10Minutes(int value) { withdrawalPer10Minutes = positive(value); }
    public int getLogoutAllPerMinute() { return logoutAllPerMinute; }
    public void setLogoutAllPerMinute(int value) { logoutAllPerMinute = positive(value); }
    public int getAdminMutationPerMinute() { return adminMutationPerMinute; }
    public void setAdminMutationPerMinute(int value) { adminMutationPerMinute = positive(value); }
    public int getMaximumKeys() { return maximumKeys; }
    public void setMaximumKeys(int value) { maximumKeys = positive(value); }

    private static int positive(int value) {
        if (value < 1) throw new IllegalArgumentException("Rate limit values must be positive");
        return value;
    }
}
