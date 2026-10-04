package com.moneybook.backend.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Logs masked verification details only for the explicitly local runtime profile. */
@Component
@Profile("local & !prod & !test")
public class LocalEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(LocalEmailSender.class);

    @Override
    public void sendVerificationCode(String email, String code, String purpose) {
        log.info("[LOCAL EMAIL VERIFICATION] purpose={} email={} verificationCode={}",
                purpose, maskEmail(email), code);
    }

    static String maskEmail(String email) {
        int separator = email.indexOf('@');
        if (separator <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(separator);
    }
}
