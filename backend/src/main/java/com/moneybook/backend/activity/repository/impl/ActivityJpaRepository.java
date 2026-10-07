package com.moneybook.backend.activity.repository.impl;

import com.moneybook.backend.entity.MoneyBookActivity;
import org.springframework.data.jpa.repository.JpaRepository;

interface ActivityJpaRepository extends JpaRepository<MoneyBookActivity, Long> {
}
