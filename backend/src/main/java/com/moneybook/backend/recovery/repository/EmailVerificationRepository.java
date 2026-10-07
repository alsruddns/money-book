package com.moneybook.backend.recovery.repository;
import com.moneybook.backend.entity.EmailVerification;
import java.util.Optional;
public interface EmailVerificationRepository {
 EmailVerification save(EmailVerification verification);
 Optional<EmailVerification> findByUid(Long uid);
 Optional<EmailVerification> findByUidForUpdate(Long uid);
 Optional<EmailVerification> findLatestActive(String email,String purpose,Long userUid);
 Optional<EmailVerification> findActiveGrant(String digest,String purpose);
}
