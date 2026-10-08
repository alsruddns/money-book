package com.moneybook.backend.board.repository.impl;

import com.moneybook.backend.entity.BoardPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class BoardPostSearchRepositoryImpl implements BoardPostSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 게시글 목록을 필수 공개 범위와 전달된 선택 조건으로 조회하고 별도 count 쿼리로 페이지 정보를 계산한다.
     */
    @Override
    public Page<BoardPost> search(Long categoryUid, String keyword, Long viewerUid,
                                  boolean superAdmin, Pageable pageable) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<BoardPost> query = builder.createQuery(BoardPost.class);
        Root<BoardPost> post = query.from(BoardPost.class);
        post.fetch("category");

        List<Predicate> predicates = predicates(builder, post, categoryUid, keyword, viewerUid, superAdmin);
        query.select(post).where(predicates.toArray(Predicate[]::new));
        query.orderBy(pageable.getSort().stream()
                .map(order -> order.isAscending()
                        ? builder.asc(post.get(order.getProperty()))
                        : builder.desc(post.get(order.getProperty())))
                .toList());

        TypedQuery<BoardPost> dataQuery = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());
        List<BoardPost> content = dataQuery.getResultList();

        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<BoardPost> countPost = countQuery.from(BoardPost.class);
        countQuery.select(builder.count(countPost)).where(predicates(builder, countPost, categoryUid,
                keyword, viewerUid, superAdmin).toArray(Predicate[]::new));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> predicates(CriteriaBuilder builder, Root<BoardPost> post,
                                       Long categoryUid, String keyword, Long viewerUid,
                                       boolean superAdmin) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.isFalse(post.get("deleted")));
        if (categoryUid != null) {
            predicates.add(builder.equal(post.get("category").get("categoryUid"), categoryUid));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
            Predicate textMatch = builder.or(
                    builder.like(builder.lower(post.get("title")), pattern),
                    builder.like(builder.lower(post.get("content")), pattern));
            Predicate readableForSearch = superAdmin
                    ? builder.conjunction()
                    : builder.or(builder.isFalse(post.get("secret")),
                            builder.equal(post.get("authorUserUid"), viewerUid));
            predicates.add(builder.and(readableForSearch, textMatch));
        }
        return predicates;
    }
}
