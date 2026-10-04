package com.moneybook.backend.recovery.repository.impl;
import com.moneybook.backend.entity.EmailVerification;
import com.moneybook.backend.recovery.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository @RequiredArgsConstructor
public class EmailVerificationRepositoryImpl implements EmailVerificationRepository {
 private final EmailVerificationJpaRepository jpa;
 @Override public EmailVerification save(EmailVerification v){return jpa.save(v);}
 @Override public Optional<EmailVerification> findByUid(Long uid){return jpa.findById(uid);}
 @Override public Optional<EmailVerification> findByUidForUpdate(Long uid){return jpa.findForUpdate(uid);}
 @Override public Optional<EmailVerification> findLatestActive(String email,String purpose,Long uid){return jpa.findLatest(email,purpose,uid).stream().findFirst();}
 @Override public Optional<EmailVerification> findActiveGrant(String digest,String purpose){return jpa.findGrant(digest,purpose);}
}
