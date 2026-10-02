package com.moneybook.backend.session.repository.impl;

import com.moneybook.backend.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** JPA adapter used by RefreshSessionRepositoryImpl. */
public interface RefreshSessionJpaRepository extends JpaRepository<RefreshTokenSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSession session where session.sessionKey = :sessionKey")
    Optional<RefreshTokenSession> findForUpdateBySessionKey(@Param("sessionKey") String sessionKey);

    Optional<RefreshTokenSession> findByRefreshSessionUidAndUserUid(Long refreshSessionUid, Long userUid);

    @Query("select session from RefreshTokenSession session where session.userUid = :userUid "
            + "and session.revokedAt is null and session.expiresAt > :now "
            + "order by session.lastUsedAt desc, session.refreshSessionUid desc")
    List<RefreshTokenSession> findActiveByUserUid(@Param("userUid") Long userUid,
                                                   @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSession session where session.userUid = :userUid "
            + "and session.revokedAt is null order by session.refreshSessionUid asc")
    List<RefreshTokenSession> findUnrevokedByUserUidForUpdate(@Param("userUid") Long userUid);
}
