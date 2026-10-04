package com.moneybook.backend.recovery;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SmtpEmailSender implements EmailSender {
 private final JavaMailSender sender; private final String from;
 public SmtpEmailSender(JavaMailSender sender,@Value("${app.mail.from:${MAIL_FROM:no-reply@moneybook.local}}") String from){this.sender=sender;this.from=from;}
 @Override public void sendVerificationCode(String email,String code){SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);message.setSubject("MoneyBook 이메일 인증");message.setText("인증번호: "+code+"\n10분 안에 입력해 주세요.");sender.send(message);}
}
