package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MoneyBookUserRepositoryImpl implements MoneyBookUserRepository {

    private final MoneyBookUserJpaRepository moneyBookUserJpaRepository;

    @Override
    public MoneyBookUser save(MoneyBookUser membership) {
        return moneyBookUserJpaRepository.save(membership);
    }

    @Override
    public List<MoneyBookUser> findReadableAcceptedByUserUid(Long userUid) {
        return moneyBookUserJpaRepository.findReadableByUserUidAndStatus(userUid, InvitationStatus.ACCEPTED);
    }
}
