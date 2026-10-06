package com.moneybook.backend.user.repository.impl;

import com.moneybook.backend.entity.User;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long userUid) {
        return userJpaRepository.findById(userUid);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long userUid) {
        return userJpaRepository.findForUpdate(userUid);
    }

    @Override
    public List<User> findByIdsForUpdate(List<Long> userUids) {
        return userJpaRepository.findForUpdateInUidOrder(userUids);
    }

    @Override
    public List<User> findAllByIds(List<Long> userUids) {
        return userJpaRepository.findAllById(userUids);
    }
}
