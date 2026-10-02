package com.moneybook.backend.admin.repository.impl;

import com.moneybook.backend.admin.dto.*;
import com.moneybook.backend.admin.repository.AdminReadRepository;
import com.moneybook.backend.enums.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 관리자 화면에 필요한 제한된 projection과 DB 집계를 제공한다. */
@Repository
@RequiredArgsConstructor
public class AdminReadRepositoryImpl implements AdminReadRepository {
    private final EntityManager em;

    @Override
    public Page<AdminUserResponse> users(String keyword, UserStatus status, SystemRole role, Pageable pageable) {
        String pattern = pattern(keyword);
        String where = " where (:status is null or u.status=:status) and (:role is null or u.systemRole=:role) " +
                "and (:pattern is null or lower(u.nickname) like :pattern escape '!' or exists " +
                "(select a.userAuthUid from UserAuth a where a.user=u and a.provider=:local and " +
                "lower(a.loginId) like :pattern escape '!'))";
        String query = "select new com.moneybook.backend.admin.dto.AdminUserResponse(u.userUid, " +
                "(select min(a.loginId) from UserAuth a where a.user=u and a.provider=:local), " +
                "u.nickname,u.status,u.systemRole,u.regTime) from User u" + where +
                " order by u.regTime desc,u.userUid desc";
        var rows = em.createQuery(query, AdminUserResponse.class)
                .setParameter("status", status).setParameter("role", role)
                .setParameter("pattern", pattern).setParameter("local", AuthProvider.LOCAL)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
        long total = em.createQuery("select count(u.userUid) from User u" + where, Long.class)
                .setParameter("status", status).setParameter("role", role)
                .setParameter("pattern", pattern).setParameter("local", AuthProvider.LOCAL).getSingleResult();
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public Optional<AdminUserDetailResponse> user(Long uid) {
        var rows = em.createQuery("select new com.moneybook.backend.admin.dto.AdminUserDetailResponse(" +
                "u.userUid,(select min(a.loginId) from UserAuth a where a.user=u and a.provider=:local)," +
                "u.nickname,u.status,u.systemRole,u.regTime,u.modTime," +
                "(select count(b.moneyBookUid) from MoneyBook b where b.ownerUserUid=u.userUid)," +
                "(select count(m.moneyBookUserUid) from MoneyBookUser m where m.userUid=u.userUid " +
                "and m.invitationStatus=:accepted and m.moneyBook.ownerUserUid<>u.userUid)) " +
                "from User u where u.userUid=:uid", AdminUserDetailResponse.class)
                .setParameter("local", AuthProvider.LOCAL).setParameter("accepted", InvitationStatus.ACCEPTED)
                .setParameter("uid", uid).getResultList();
        return rows.stream().findFirst();
    }

    @Override
    public Page<AdminMoneyBookResponse> moneyBooks(String keyword, Long ownerUid, Pageable pageable) {
        String pattern = pattern(keyword);
        String where = " where (:pattern is null or lower(b.name) like :pattern escape '!') " +
                "and (:owner is null or b.ownerUserUid=:owner)";
        String select = "select new com.moneybook.backend.admin.dto.AdminMoneyBookResponse(b.moneyBookUid,b.name," +
                "b.ownerUserUid,u.nickname,count(distinct m.moneyBookUserUid),b.regTime," +
                "(select max(a.occurredAt) from MoneyBookActivity a where a.moneyBookUid=b.moneyBookUid)) " +
                "from MoneyBook b join User u on u.userUid=b.ownerUserUid " +
                "left join MoneyBookUser m on m.moneyBook=b and m.invitationStatus=:accepted" + where +
                " group by b.moneyBookUid,b.name,b.ownerUserUid,u.nickname,b.regTime " +
                "order by b.regTime desc,b.moneyBookUid desc";
        var rows = em.createQuery(select, AdminMoneyBookResponse.class)
                .setParameter("accepted", InvitationStatus.ACCEPTED).setParameter("pattern", pattern)
                .setParameter("owner", ownerUid).setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList();
        long total = em.createQuery("select count(b.moneyBookUid) from MoneyBook b" + where, Long.class)
                .setParameter("pattern", pattern).setParameter("owner", ownerUid).getSingleResult();
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public Optional<AdminMoneyBookDetailResponse> moneyBook(Long uid) {
        var rows = em.createQuery("select new com.moneybook.backend.admin.dto.AdminMoneyBookDetailResponse(" +
                "b.moneyBookUid,b.name,b.ownerUserUid,u.nickname," +
                "(select count(m.moneyBookUserUid) from MoneyBookUser m where m.moneyBook=b and m.invitationStatus=:accepted)," +
                "(select count(c.categoryUid) from MoneyBookCategory c where c.moneyBook=b)," +
                "(select count(a.accountUid) from MoneyBookAccount a where a.moneyBook=b)," +
                "(select count(t.transactionUid) from MoneyBookTransaction t where t.moneyBook=b)," +
                "(select count(x.transferUid) from MoneyBookTransfer x where x.moneyBook=b)," +
                "(select count(c.closingUid) from MoneyBookMonthClosing c where c.moneyBook=b),b.regTime," +
                "(select max(a.occurredAt) from MoneyBookActivity a where a.moneyBookUid=b.moneyBookUid)) " +
                "from MoneyBook b join User u on u.userUid=b.ownerUserUid where b.moneyBookUid=:uid",
                AdminMoneyBookDetailResponse.class).setParameter("accepted", InvitationStatus.ACCEPTED)
                .setParameter("uid", uid).getResultList();
        return rows.stream().findFirst();
    }

    @Override
    public List<Object[]> moneyBookNames(List<Long> uids) {
        if (uids.isEmpty()) return List.of();
        return em.createQuery("select b.moneyBookUid,b.name from MoneyBook b where b.moneyBookUid in :uids", Object[].class)
                .setParameter("uids", uids).getResultList();
    }

    @Override
    public long countActivitiesBetween(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select count(a.activityUid) from MoneyBookActivity a " +
                "where a.occurredAt>=:start and a.occurredAt<:end", Long.class)
                .setParameter("start", start).setParameter("end", end).getSingleResult();
    }

    @Override
    public AdminOverviewResponse overview() {
        Object[] counts = em.createQuery("select count(u.userUid)," +
                "sum(case when u.status=:active then 1 else 0 end),sum(case when u.status=:blocked then 1 else 0 end)," +
                "sum(case when u.systemRole=:admin then 1 else 0 end),sum(case when u.systemRole=:super then 1 else 0 end) " +
                "from User u", Object[].class).setParameter("active", UserStatus.ACTIVE)
                .setParameter("blocked", UserStatus.BLOCKED).setParameter("admin", SystemRole.SYSTEM_ADMIN)
                .setParameter("super", SystemRole.SUPER_ADMIN).getSingleResult();
        long books = em.createQuery("select count(b.moneyBookUid) from MoneyBook b", Long.class).getSingleResult();
        LocalDateTime today = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDate().atStartOfDay();
        long todayActivities = countActivitiesBetween(today, today.plusDays(1));
        return new AdminOverviewResponse(number(counts[0]), number(counts[1]), number(counts[2]),
                number(counts[3]), number(counts[4]), books, todayActivities);
    }

    private long number(Object value) { return value == null ? 0L : ((Number) value).longValue(); }
    private String pattern(String value) {
        if (value == null || value.isBlank()) return null;
        return "%" + value.toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
