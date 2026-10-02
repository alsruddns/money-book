package com.moneybook.backend.user.repository.impl;

import com.moneybook.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Spring Data JPA persistence adapter used only by UserRepositoryImpl. */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.userUid = :userUid")
    Optional<User> findForUpdate(@Param("userUid") Long userUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.userUid in :userUids order by user.userUid asc")
    List<User> findForUpdateInUidOrder(@Param("userUids") List<Long> userUids);
}
