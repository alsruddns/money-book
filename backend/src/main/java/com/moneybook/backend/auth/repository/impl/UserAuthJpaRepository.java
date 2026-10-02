package com.moneybook.backend.auth.repository.impl;

import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Spring Data JPA queries used only by UserAuthRepositoryImpl. */
public interface UserAuthJpaRepository extends JpaRepository<UserAuth, Long> {

    Optional<UserAuth> findByProviderAndLoginId(AuthProvider provider, String loginId);

    Optional<UserAuth> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    List<UserAuth> findByUser_UserUid(Long userUid);

    @Modifying
    @Query("delete from UserAuth auth where auth.user.userUid = :userUid")
    int deleteAllForUser(@Param("userUid") Long userUid);
}
