package com.moneybook.backend.user.repository.impl;

import com.moneybook.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA persistence adapter used only by UserRepositoryImpl. */
public interface UserJpaRepository extends JpaRepository<User, Long> {
}
