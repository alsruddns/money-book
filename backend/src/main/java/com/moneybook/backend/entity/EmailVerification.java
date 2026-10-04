package com.moneybook.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name="account_email_verifications") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="email_verification_uid") private Long emailVerificationUid;
    @Column(name="user_uid") private Long userUid;
    @Column(nullable=false,length=254) private String email;
    @Column(name="purpose",nullable=false,length=30) private String purpose;
    @Column(name="code_hash",nullable=false,length=64) private String codeHash;
    @Column(name="grant_hash",length=64) private String grantHash;
    @Column(name="grant_expires_at") private LocalDateTime grantExpiresAt;
    @Column(name="grant_consumed_at") private LocalDateTime grantConsumedAt;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
    @Column(name="resend_after",nullable=false) private LocalDateTime resendAfter;
    @Column(name="attempt_count",nullable=false) private int attemptCount;
    @Column(name="consumed_at") private LocalDateTime consumedAt;
    public EmailVerification(Long uid,String email,String purpose,String codeHash,LocalDateTime expiresAt,LocalDateTime resendAfter){this.userUid=uid;this.email=email;this.purpose=purpose;this.codeHash=codeHash;this.expiresAt=expiresAt;this.resendAfter=resendAfter;}
    public boolean active(LocalDateTime now){return consumedAt==null&&expiresAt.isAfter(now)&&attemptCount<5;}
    public void failedAttempt(){attemptCount++; if(attemptCount>=5) consumedAt=LocalDateTime.now();}
    public void invalidate(LocalDateTime now){if(consumedAt==null)consumedAt=now;}
    public void issueGrant(String digest,LocalDateTime expiresAt){grantHash=digest;grantExpiresAt=expiresAt;consumedAt=LocalDateTime.now();}
    public boolean grantActive(LocalDateTime now){return grantHash!=null&&grantConsumedAt==null&&grantExpiresAt.isAfter(now);}
    public void consumeGrant(LocalDateTime now){if(!grantActive(now))throw new IllegalStateException("Verification grant is no longer usable");grantConsumedAt=now;}
}
