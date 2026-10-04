package com.moneybook.backend.recovery.repository.impl;
import com.moneybook.backend.entity.PasswordRecoveryCode;
import com.moneybook.backend.recovery.repository.PasswordRecoveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository @RequiredArgsConstructor
public class PasswordRecoveryRepositoryImpl implements PasswordRecoveryRepository {
 private final PasswordRecoveryJpaRepository jpa;
 @Override public PasswordRecoveryCode save(PasswordRecoveryCode c){return jpa.save(c);}
 @Override public List<PasswordRecoveryCode> findByUserUid(Long uid){return jpa.findByUserUid(uid);}
 @Override public Optional<PasswordRecoveryCode> findUsableByHash(String hash){return jpa.findUsable(hash);}
}
