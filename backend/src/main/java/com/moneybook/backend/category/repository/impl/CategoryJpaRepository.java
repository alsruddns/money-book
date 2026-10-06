package com.moneybook.backend.category.repository.impl;

import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CategoryJpaRepository extends JpaRepository<MoneyBookCategory, Long> {
    Optional<MoneyBookCategory> findByCategoryUidAndMoneyBook_MoneyBookUid(Long categoryUid, Long moneyBookUid);
    List<MoneyBookCategory> findByMoneyBook_MoneyBookUidAndCategoryUidIn(Long moneyBookUid, Set<Long> categoryUids);

    List<MoneyBookCategory> findByMoneyBook_MoneyBookUidOrderByTransactionTypeAscSortOrderAscCategoryUidAsc(
            Long moneyBookUid);

    List<MoneyBookCategory> findByMoneyBook_MoneyBookUidAndTransactionTypeOrderByTransactionTypeAscSortOrderAscCategoryUidAsc(
            Long moneyBookUid, TransactionType transactionType);

    @Query("""
            select (count(category) > 0) from MoneyBookCategory category
            where category.moneyBook.moneyBookUid = :bookUid
              and category.transactionType = :type and category.name = :name
              and (:exceptUid is null or category.categoryUid <> :exceptUid)
            """)
    boolean existsDuplicate(@Param("bookUid") Long bookUid, @Param("type") TransactionType type,
                            @Param("name") String name, @Param("exceptUid") Long exceptUid);

    @Query("select (count(entry) > 0) from MoneyBookTransaction entry where entry.category.categoryUid = :categoryUid")
    boolean isInUse(@Param("categoryUid") Long categoryUid);
}
