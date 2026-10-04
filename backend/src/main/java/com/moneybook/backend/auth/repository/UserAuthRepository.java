package com.moneybook.backend.auth.repository;

import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.AuthProvider;

import java.util.List;
import java.util.Optional;

public interface UserAuthRepository {

    UserAuth save(UserAuth userAuth);

    /** Finds a LOCAL identity by its login ID without using an email address. */
    Optional<UserAuth> findByLocalLoginId(String loginId);

    /** Finds a provider identity by its provider-specific user ID. */
    Optional<UserAuth> findByProviderUserId(AuthProvider provider, String providerUserId);

    List<UserAuth> findByUserUid(Long userUid);

    Optional<UserAuth> findLocalByUserUid(Long userUid);

    Optional<UserAuth> findLocalByUserUidForUpdate(Long userUid);

    /** Checks whether any LOCAL credential already owns a normalized verified email. */
    boolean existsVerifiedEmail(String verifiedEmail);

    /** Checks email ownership while excluding the account currently being updated. */
    boolean existsVerifiedEmailForAnotherUser(String verifiedEmail, Long userUid);

    int deleteAllByUserUid(Long userUid);
}
