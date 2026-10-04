package com.moneybook.backend.board.repository.impl;
import com.moneybook.backend.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface BoardCommentJpaRepository extends JpaRepository<BoardComment,Long> {
 List<BoardComment> findByPost_PostUidAndParentIsNullOrderByRegTimeAscCommentUidAsc(Long postUid);
 List<BoardComment> findByPost_PostUidOrderByRegTimeAscCommentUidAsc(Long postUid);
 List<BoardComment> findByParent_CommentUidOrderByRegTimeAscCommentUidAsc(Long parentUid);
 long countByPost_PostUidAndDeletedFalse(Long postUid);
 boolean existsByParent_CommentUid(Long uid);
 @Query("select c.post.postUid, count(c.commentUid) from BoardComment c where c.deleted=false and c.post.postUid in :postUids group by c.post.postUid")
 List<Object[]> countByPostUids(@Param("postUids") List<Long> postUids);
}
