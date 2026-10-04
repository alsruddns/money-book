package com.moneybook.backend.board.repository.impl;
import com.moneybook.backend.entity.BoardPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface BoardPostJpaRepository extends JpaRepository<BoardPost,Long> {
 @Query("select p from BoardPost p join fetch p.category where p.deleted=false and (:category is null or p.category.categoryUid=:category) and (:keyword is null or (lower(p.title) like lower(concat('%',:keyword,'%')) or (p.secret=false and lower(p.content) like lower(concat('%',:keyword,'%')))))")
 Page<BoardPost> search(@Param("category") Long category, @Param("keyword") String keyword, Pageable pageable);
 long countByCategory_CategoryUid(Long uid);
}
