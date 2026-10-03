package com.moneybook.backend.session.repository;

import com.moneybook.backend.entity.RefreshTokenSession;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface RefreshSessionRepository {
    RefreshTokenSession save(RefreshTokenSession session);
    Optional<RefreshTokenSession> findBySessionKeyForUpdate(String sessionKey);
    Optional<RefreshTokenSession> findByUidAndUserUid(Long sessionUid, Long userUid);
    List<RefreshTokenSession> findActiveByUserUid(Long userUid);
    List<RefreshTokenSession> findUnrevokedByUserUid(Long userUid);
    List<RefreshTokenSession> findActiveByUserUidForUpdate(Long userUid, LocalDateTime now);
}
