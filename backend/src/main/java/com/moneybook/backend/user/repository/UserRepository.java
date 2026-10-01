package com.moneybook.backend.user.repository;

import com.moneybook.backend.entity.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long userUid);
}
