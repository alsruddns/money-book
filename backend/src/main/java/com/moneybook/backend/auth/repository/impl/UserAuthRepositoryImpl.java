package com.moneybook.backend.auth.repository.impl;

import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserAuthRepositoryImpl implements UserAuthRepository {

    private final UserAuthJpaRepository userAuthJpaRepository;

    @Override
    public UserAuth save(UserAuth userAuth) {
        return userAuthJpaRepository.saveAndFlush(userAuth);
    }

    @Override
    public Optional<UserAuth> findByLocalLoginId(String loginId) {
        return userAuthJpaRepository.findByProviderAndLoginId(AuthProvider.LOCAL, loginId);
    }

    @Override
    public Optional<UserAuth> findByProviderUserId(AuthProvider provider, String providerUserId) {
        return userAuthJpaRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public List<UserAuth> findByUserUid(Long userUid) {
        return userAuthJpaRepository.findByUser_UserUid(userUid);
    }

    @Override
    public int deleteAllByUserUid(Long userUid) {
        return userAuthJpaRepository.deleteAllForUser(userUid);
    }
}
