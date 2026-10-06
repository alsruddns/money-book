package com.moneybook.backend.recovery;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
@Component
@Profile("prod")
public class SmtpEmailSender implements EmailSender {
 private final JavaMailSender sender; private final String from;
 public SmtpEmailSender(JavaMailSender sender,@Value("${spring.mail.host:${MAIL_HOST:}}") String host,
                        @Value("${app.mail.from:${MAIL_FROM:}}") String from){this.sender=sender;this.host=host;this.from=from;}
 private final String host;
 @Override public void sendVerificationCode(String email,String code,String purpose){
  if(host.isBlank()||from.isBlank()) throw new IllegalStateException("Production email verification requires MAIL_HOST and MAIL_FROM");
  SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);message.setSubject("MoneyBook 이메일 인증");message.setText("인증번호: "+code+"\n10분 안에 입력해 주세요.");sender.send(message);
 }
}
