package com.moneybook.backend.user.repository;

import com.moneybook.backend.entity.User;

import java.util.Optional;
import java.util.List;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long userUid);

    Optional<User> findByIdForUpdate(Long userUid);

    /** Locks multiple users in UID order to serialize ownership transfer against withdrawal. */
    List<User> findByIdsForUpdate(List<Long> userUids);

    /** Fetches a set of users in one query for privacy-safe Board author labels. */
    List<User> findAllByIds(List<Long> userUids);
}
