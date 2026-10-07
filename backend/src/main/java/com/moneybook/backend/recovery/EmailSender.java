package com.moneybook.backend.recovery;

public interface EmailSender {
    void sendVerificationCode(String email, String code, String purpose);
}
