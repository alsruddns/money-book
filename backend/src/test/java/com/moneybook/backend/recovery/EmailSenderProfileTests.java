package com.moneybook.backend.recovery;

import com.moneybook.backend.config.MailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailSenderProfileTests {
    @Test
    void localProfileSelectsLocalSenderWithoutSmtpTransport() {
        try (var context = context("local")) {
            assertEquals(LocalEmailSender.class, context.getBean(EmailSender.class).getClass());
            assertTrue(context.getBeansOfType(JavaMailSenderImpl.class).isEmpty());
            assertEquals("s***@example.test", LocalEmailSender.maskEmail("someone@example.test"));
        }
    }

    @Test
    void testProfileSelectsSilentSenderInsteadOfLoggingVerificationSecrets() {
        try (var context = context("test")) {
            EmailSender sender = context.getBean(EmailSender.class);
            assertEquals(TestEmailSender.class, sender.getClass());
            sender.sendVerificationCode("member@example.test", "123456", "SIGNUP");
        }
    }

    @Test
    void productionProfileSelectsSmtpAndFailsClearlyWhenHostIsMissing() {
        try (var context = context("prod")) {
            EmailSender sender = context.getBean(EmailSender.class);
            assertEquals(SmtpEmailSender.class, sender.getClass());
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> sender.sendVerificationCode("member@example.test", "123456", "SIGNUP"));
            assertTrue(error.getMessage().contains("MAIL_HOST"));
        }
    }

    private AnnotationConfigApplicationContext context(String profile) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.register(MailConfig.class, LocalEmailSender.class, TestEmailSender.class,
                SmtpEmailSender.class);
        context.refresh();
        return context;
    }
}
