package com.moneybook.backend.board.repository.impl;
import com.moneybook.backend.entity.BoardPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface BoardPostJpaRepository extends JpaRepository<BoardPost,Long>, BoardPostSearchRepository {
 @Query("select p from BoardPost p join fetch p.category where p.postUid=:uid and p.deleted=false")
 java.util.Optional<BoardPost> findActiveWithCategory(@Param("uid") Long uid);
 long countByCategory_CategoryUid(Long uid);
}
