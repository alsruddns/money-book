package com.moneybook.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

/** Configures the SMTP transport exclusively through environment-backed properties. */
@Configuration
public class MailConfig {
 @Bean public JavaMailSender javaMailSender(
  @Value("${spring.mail.host:${MAIL_HOST:}}") String host,
  @Value("${spring.mail.port:${MAIL_PORT:25}}") int port,
  @Value("${spring.mail.username:${MAIL_USERNAME:}}") String username,
  @Value("${spring.mail.password:${MAIL_PASSWORD:}}") String password){
  JavaMailSenderImpl sender=new JavaMailSenderImpl();sender.setHost(host);sender.setPort(port);
  if(!username.isBlank())sender.setUsername(username);if(!password.isBlank())sender.setPassword(password);
  Properties props=sender.getJavaMailProperties();props.put("mail.smtp.auth",!username.isBlank());
  props.put("mail.smtp.starttls.enable",true);props.put("mail.smtp.connectiontimeout","5000");
  props.put("mail.smtp.timeout","5000");props.put("mail.smtp.writetimeout","5000");return sender;
 }
}
