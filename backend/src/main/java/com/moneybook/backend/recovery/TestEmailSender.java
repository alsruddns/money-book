package com.moneybook.backend.recovery;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Deliberately discards email in tests so verification secrets never enter test logs. */
@Component
@Profile("test")
public class TestEmailSender implements EmailSender {
    @Override
    public void sendVerificationCode(String email, String code, String purpose) {
        // Tests assert the persisted verification flow without sending or logging credentials.
    }
}
