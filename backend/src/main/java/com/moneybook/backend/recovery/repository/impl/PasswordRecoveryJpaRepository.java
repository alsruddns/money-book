package com.moneybook.backend.recovery.repository.impl;
import com.moneybook.backend.entity.PasswordRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface PasswordRecoveryJpaRepository extends JpaRepository<PasswordRecoveryCode,Long> {
 List<PasswordRecoveryCode> findByUserUid(Long uid);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select c from PasswordRecoveryCode c where c.codeHash=:hash and c.usedAt is null and c.revokedAt is null") Optional<PasswordRecoveryCode> findUsable(@Param("hash") String hash);
}
