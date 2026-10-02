package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MoneyBookRepositoryImpl implements MoneyBookRepository {

    private final MoneyBookJpaRepository moneyBookJpaRepository;

    @Override
    public MoneyBook save(MoneyBook moneyBook) {
        return moneyBookJpaRepository.save(moneyBook);
    }

    @Override
    public Optional<MoneyBook> findById(Long moneyBookUid) {
        return moneyBookJpaRepository.findById(moneyBookUid);
    }

    @Override
    public Optional<MoneyBook> findByIdForUpdate(Long moneyBookUid) {
        return moneyBookJpaRepository.findForUpdate(moneyBookUid);
    }

    @Override
    public long countOwnedByUserUid(Long userUid) {
        return moneyBookJpaRepository.countByOwnerUserUid(userUid);
    }
}
