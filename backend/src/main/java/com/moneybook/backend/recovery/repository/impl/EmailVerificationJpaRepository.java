package com.moneybook.backend.recovery.repository.impl;
import com.moneybook.backend.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerification,Long> {
 @Query("select v from EmailVerification v where v.email=:email and v.purpose=:purpose and ((:uid is null and v.userUid is null) or v.userUid=:uid) and v.consumedAt is null order by v.regTime desc")
 java.util.List<EmailVerification> findLatest(@Param("email")String email,@Param("purpose")String purpose,@Param("uid")Long uid);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select v from EmailVerification v where v.grantHash=:digest and v.purpose=:purpose and v.grantConsumedAt is null") Optional<EmailVerification> findGrant(@Param("digest")String digest,@Param("purpose")String purpose);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select v from EmailVerification v where v.emailVerificationUid=:uid") Optional<EmailVerification> findForUpdate(@Param("uid")Long uid);
}
