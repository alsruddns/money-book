package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBook;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter used by MoneyBookRepositoryImpl. */
public interface MoneyBookJpaRepository extends JpaRepository<MoneyBook, Long> {
}
