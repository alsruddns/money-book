package com.moneybook.backend.board.repository.impl;
import com.moneybook.backend.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoardCommentJpaRepository extends JpaRepository<BoardComment,Long> {
 List<BoardComment> findByPost_PostUidAndParentIsNullOrderByRegTimeAscCommentUidAsc(Long postUid);
 List<BoardComment> findByParent_CommentUidOrderByRegTimeAscCommentUidAsc(Long parentUid);
 long countByPost_PostUidAndDeletedFalse(Long postUid);
 boolean existsByParent_CommentUid(Long uid);
}
