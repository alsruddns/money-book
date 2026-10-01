package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MoneyBookRepositoryImpl implements MoneyBookRepository {

    private final MoneyBookJpaRepository moneyBookJpaRepository;

    @Override
    public MoneyBook save(MoneyBook moneyBook) {
        return moneyBookJpaRepository.save(moneyBook);
    }
}
