package com.moneybook.backend.activity.repository.impl;

import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.entity.MoneyBookActivity;
import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepository {
    private final ActivityJpaRepository jpa;
    private final EntityManager entityManager;
    @Override public MoneyBookActivity save(MoneyBookActivity activity) { return jpa.save(activity); }
    @Override public Page<MoneyBookActivity> search(Long bookUid, LocalDate start, LocalDate end, Long actor,
                                                    ActivityType type, ActivityTargetType target, Pageable pageable) {
        LocalDateTime startAt = start == null ? null : start.atStartOfDay();
        LocalDateTime endExclusive = end == null ? null : end.plusDays(1).atStartOfDay();
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(MoneyBookActivity.class);
        var root = query.from(MoneyBookActivity.class);
        var predicates = new ArrayList<Predicate>();
        if (bookUid != null) predicates.add(cb.equal(root.get("moneyBookUid"), bookUid));
        if (startAt != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), startAt));
        if (endExclusive != null) predicates.add(cb.lessThan(root.get("occurredAt"), endExclusive));
        if (actor != null) predicates.add(cb.equal(root.get("actorUserUid"), actor));
        if (type != null) predicates.add(cb.equal(root.get("activityType"), type));
        if (target != null) predicates.add(cb.equal(root.get("targetType"), target));
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(cb.desc(root.get("occurredAt")), cb.desc(root.get("activityUid")));
        var rows = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();

        var countQuery = cb.createQuery(Long.class);
        var countRoot = countQuery.from(MoneyBookActivity.class);
        var countPredicates = new ArrayList<Predicate>();
        if (bookUid != null) countPredicates.add(cb.equal(countRoot.get("moneyBookUid"), bookUid));
        if (startAt != null) countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("occurredAt"), startAt));
        if (endExclusive != null) countPredicates.add(cb.lessThan(countRoot.get("occurredAt"), endExclusive));
        if (actor != null) countPredicates.add(cb.equal(countRoot.get("actorUserUid"), actor));
        if (type != null) countPredicates.add(cb.equal(countRoot.get("activityType"), type));
        if (target != null) countPredicates.add(cb.equal(countRoot.get("targetType"), target));
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(Predicate[]::new));
        return new PageImpl<>(rows, pageable, entityManager.createQuery(countQuery).getSingleResult());
    }
}
