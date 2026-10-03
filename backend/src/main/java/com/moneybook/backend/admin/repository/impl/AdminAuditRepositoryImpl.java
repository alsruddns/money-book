package com.moneybook.backend.admin.repository.impl;

import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.entity.SystemAdminAuditLog;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Repository
@RequiredArgsConstructor
public class AdminAuditRepositoryImpl implements AdminAuditRepository {
    private final AdminAuditJpaRepository jpa;
    private final EntityManager entityManager;
    @Override public SystemAdminAuditLog save(SystemAdminAuditLog log) { return jpa.save(log); }
    @Override public Page<SystemAdminAuditLog> search(Long actor, Long targetUserUid, AdminAuditActionType action,
            AdminAuditTargetType target, LocalDate start, LocalDate end, Pageable pageable) {
        LocalDateTime startAt = start == null ? null : start.atStartOfDay();
        LocalDateTime endExclusive = end == null ? null : end.plusDays(1).atStartOfDay();
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(SystemAdminAuditLog.class);
        var root = query.from(SystemAdminAuditLog.class);
        var predicates = new ArrayList<Predicate>();
        if (actor != null) predicates.add(cb.equal(root.get("actorUserUid"), actor));
        if (targetUserUid != null) {
            predicates.add(cb.equal(root.get("targetType"), AdminAuditTargetType.USER));
            predicates.add(cb.equal(root.get("targetUid"), targetUserUid));
        }
        if (action != null) predicates.add(cb.equal(root.get("actionType"), action));
        if (target != null) predicates.add(cb.equal(root.get("targetType"), target));
        if (startAt != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), startAt));
        if (endExclusive != null) predicates.add(cb.lessThan(root.get("occurredAt"), endExclusive));
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(cb.desc(root.get("occurredAt")), cb.desc(root.get("adminAuditLogUid")));
        var rows = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();

        var countQuery = cb.createQuery(Long.class);
        var countRoot = countQuery.from(SystemAdminAuditLog.class);
        var countPredicates = new ArrayList<Predicate>();
        if (actor != null) countPredicates.add(cb.equal(countRoot.get("actorUserUid"), actor));
        if (targetUserUid != null) {
            countPredicates.add(cb.equal(countRoot.get("targetType"), AdminAuditTargetType.USER));
            countPredicates.add(cb.equal(countRoot.get("targetUid"), targetUserUid));
        }
        if (action != null) countPredicates.add(cb.equal(countRoot.get("actionType"), action));
        if (target != null) countPredicates.add(cb.equal(countRoot.get("targetType"), target));
        if (startAt != null) countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("occurredAt"), startAt));
        if (endExclusive != null) countPredicates.add(cb.lessThan(countRoot.get("occurredAt"), endExclusive));
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(Predicate[]::new));
        return new PageImpl<>(rows, pageable, entityManager.createQuery(countQuery).getSingleResult());
    }
}
