package com.moneybook.backend.backup.repository.impl;

import com.moneybook.backend.backup.repository.BackupRepository;
import com.moneybook.backend.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BackupRepositoryImpl implements BackupRepository {
    private final EntityManager entityManager;

    @Override public Optional<MoneyBook> findBook(Long uid) { return Optional.ofNullable(entityManager.find(MoneyBook.class, uid)); }

    /** Entity types are explicitly whitelisted and never derived from uploaded JSON. */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findRows(Class<T> type, Long uid) {
        if (type == MoneyBookCategory.class) return (List<T>) entityManager.createQuery("select e from MoneyBookCategory e where e.moneyBook.moneyBookUid=:uid order by e.categoryUid", type).setParameter("uid", uid).getResultList();
        if (type == MoneyBookAccount.class) return (List<T>) entityManager.createQuery("select e from MoneyBookAccount e where e.moneyBook.moneyBookUid=:uid order by e.accountUid", type).setParameter("uid", uid).getResultList();
        if (type == MoneyBookTransaction.class) return (List<T>) entityManager.createQuery("select e from MoneyBookTransaction e join fetch e.category join fetch e.account where e.moneyBook.moneyBookUid=:uid order by e.transactionUid", type).setParameter("uid", uid).getResultList();
        if (type == MoneyBookTransfer.class) return (List<T>) entityManager.createQuery("select e from MoneyBookTransfer e join fetch e.fromAccount join fetch e.toAccount where e.moneyBook.moneyBookUid=:uid order by e.transferUid", type).setParameter("uid", uid).getResultList();
        if (type == RecurringTransaction.class) return (List<T>) entityManager.createQuery("select e from RecurringTransaction e join fetch e.category join fetch e.account where e.moneyBook.moneyBookUid=:uid order by e.recurringTransactionUid", type).setParameter("uid", uid).getResultList();
        if (type == MoneyBookBudget.class) return (List<T>) entityManager.createQuery("select distinct e from MoneyBookBudget e left join fetch e.categories c left join fetch c.category where e.moneyBook.moneyBookUid=:uid order by e.year,e.month", type).setParameter("uid", uid).getResultList();
        if (type == MoneyBookMonthClosing.class) return (List<T>) entityManager.createQuery("select e from MoneyBookMonthClosing e where e.moneyBook.moneyBookUid=:uid order by e.year,e.month", type).setParameter("uid", uid).getResultList();
        if (type == MoneyBookSetting.class) return (List<T>) entityManager.createQuery("select e from MoneyBookSetting e where e.moneyBook.moneyBookUid=:uid", type).setParameter("uid", uid).getResultList();
        throw new IllegalArgumentException("Unsupported backup entity type");
    }
    @Override
    public List<MoneyBookTransaction> transactionBatch(Long uid, Long afterUid, int limit) {
        return entityManager.createQuery("select e from MoneyBookTransaction e join fetch e.category join fetch e.account where e.moneyBook.moneyBookUid=:uid and e.transactionUid>:afterUid order by e.transactionUid", MoneyBookTransaction.class)
                .setParameter("uid", uid).setParameter("afterUid", afterUid).setMaxResults(limit).getResultList();
    }
    @Override
    public List<MoneyBookTransfer> transferBatch(Long uid, Long afterUid, int limit) {
        return entityManager.createQuery("select e from MoneyBookTransfer e join fetch e.fromAccount join fetch e.toAccount where e.moneyBook.moneyBookUid=:uid and e.transferUid>:afterUid order by e.transferUid", MoneyBookTransfer.class)
                .setParameter("uid", uid).setParameter("afterUid", afterUid).setMaxResults(limit).getResultList();
    }
    @Override public <T> T save(T entity) { entityManager.persist(entity); return entity; }
    @Override public void flush() { entityManager.flush(); }
    @Override public void clear() { entityManager.clear(); }
}
