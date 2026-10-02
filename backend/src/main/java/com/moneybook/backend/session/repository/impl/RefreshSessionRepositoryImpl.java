package com.moneybook.backend.session.repository.impl;

import com.moneybook.backend.entity.RefreshTokenSession;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshSessionRepositoryImpl implements RefreshSessionRepository {
    private final RefreshSessionJpaRepository jpaRepository;

    @Override public RefreshTokenSession save(RefreshTokenSession session) { return jpaRepository.save(session); }
    @Override public Optional<RefreshTokenSession> findBySessionKeyForUpdate(String key) {
        return jpaRepository.findForUpdateBySessionKey(key);
    }
    @Override public Optional<RefreshTokenSession> findByUidAndUserUid(Long uid, Long userUid) {
        return jpaRepository.findByRefreshSessionUidAndUserUid(uid, userUid);
    }
    @Override public List<RefreshTokenSession> findActiveByUserUid(Long userUid) {
        return jpaRepository.findActiveByUserUid(userUid, LocalDateTime.now());
    }
    @Override public List<RefreshTokenSession> findUnrevokedByUserUid(Long userUid) {
        return jpaRepository.findUnrevokedByUserUidForUpdate(userUid);
    }
}
