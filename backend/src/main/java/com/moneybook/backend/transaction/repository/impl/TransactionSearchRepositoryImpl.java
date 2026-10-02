package com.moneybook.backend.transaction.repository.impl;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchSort;
import com.moneybook.backend.transaction.repository.TransactionSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
@RequiredArgsConstructor
public class TransactionSearchRepositoryImpl implements TransactionSearchRepository {
    private final EntityManager em;

    /** Both page and count share the same bound predicates; display names are fetched in the page query. */
    @Override
    public SearchResult search(Long bookUid, TransactionSearchRequest request) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MoneyBookTransaction> pageQuery = cb.createQuery(MoneyBookTransaction.class);
        Root<MoneyBookTransaction> root = pageQuery.from(MoneyBookTransaction.class);
        root.fetch("category");
        root.fetch("account");
        pageQuery.where(predicates(cb, root, bookUid, request).toArray(Predicate[]::new));
        Expression<?> primary = switch (request.sort()) {
            case DATE_DESC, DATE_ASC -> root.get("transactionDate");
            case AMOUNT_DESC, AMOUNT_ASC -> root.get("amount");
        };
        boolean descending = request.sort() == TransactionSearchSort.DATE_DESC
                || request.sort() == TransactionSearchSort.AMOUNT_DESC;
        pageQuery.orderBy(descending ? cb.desc(primary) : cb.asc(primary),
                descending ? cb.desc(root.get("transactionUid")) : cb.asc(root.get("transactionUid")));
        List<MoneyBookTransaction> rows = em.createQuery(pageQuery)
                .setFirstResult(request.page() * request.size()).setMaxResults(request.size()).getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<MoneyBookTransaction> countRoot = countQuery.from(MoneyBookTransaction.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates(cb, countRoot, bookUid, request).toArray(Predicate[]::new));
        return new SearchResult(rows, em.createQuery(countQuery).getSingleResult());
    }

    private List<Predicate> predicates(CriteriaBuilder cb, Root<MoneyBookTransaction> root, Long bookUid,
                                       TransactionSearchRequest request) {
        List<Predicate> result = new ArrayList<>();
        result.add(cb.equal(root.get("moneyBook").get("moneyBookUid"), bookUid));
        result.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), request.startDate()));
        result.add(cb.lessThanOrEqualTo(root.get("transactionDate"), request.endDate()));
        if (request.transactionType() != null) result.add(cb.equal(root.get("transactionType"), request.transactionType()));
        if (request.categoryUid() != null) result.add(cb.equal(root.get("category").get("categoryUid"), request.categoryUid()));
        if (request.accountUid() != null) result.add(cb.equal(root.get("account").get("accountUid"), request.accountUid()));
        if (request.minAmount() != null) result.add(cb.greaterThanOrEqualTo(root.get("amount"), request.minAmount()));
        if (request.maxAmount() != null) result.add(cb.lessThanOrEqualTo(root.get("amount"), request.maxAmount()));
        if (request.keyword() != null && !request.keyword().isBlank()) {
            String pattern = "%" + escapeLike(request.keyword().trim().toLowerCase(Locale.ROOT)) + "%";
            Join<Object, Object> category = root.join("category");
            Join<Object, Object> account = root.join("account");
            result.add(cb.or(cb.like(cb.lower(root.get("memo")), pattern, '!'),
                    cb.like(cb.lower(category.get("name")), pattern, '!'),
                    cb.like(cb.lower(account.get("name")), pattern, '!')));
        }
        return result;
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
