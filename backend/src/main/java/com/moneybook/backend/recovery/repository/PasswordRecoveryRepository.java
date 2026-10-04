package com.moneybook.backend.recovery.repository;
import com.moneybook.backend.entity.PasswordRecoveryCode;
import java.util.List;
import java.util.Optional;
public interface PasswordRecoveryRepository {
 PasswordRecoveryCode save(PasswordRecoveryCode code);
 List<PasswordRecoveryCode> findByUserUid(Long userUid);
 Optional<PasswordRecoveryCode> findUsableByHash(String hash);
}
