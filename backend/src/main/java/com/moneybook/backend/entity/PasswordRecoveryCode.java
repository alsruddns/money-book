package com.moneybook.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name="password_recovery_codes") @Getter @NoArgsConstructor(access= AccessLevel.PROTECTED)
public class PasswordRecoveryCode extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="recovery_code_uid") private Long recoveryCodeUid;
    @Column(name="user_uid",nullable=false) private Long userUid;
    @Column(name="code_hash",nullable=false,length=64) private String codeHash;
    @Column(name="used_at") private LocalDateTime usedAt;
    @Column(name="revoked_at") private LocalDateTime revokedAt;
    public PasswordRecoveryCode(Long userUid,String hash){this.userUid=userUid;this.codeHash=hash;}
    public boolean usable(){return usedAt==null&&revokedAt==null;}
    public void use(LocalDateTime at){if(!usable())throw new IllegalStateException("Recovery code is no longer usable");usedAt=at;}
    public void revoke(LocalDateTime at){if(usable())revokedAt=at;}
}
