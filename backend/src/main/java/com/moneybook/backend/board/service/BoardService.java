package com.moneybook.backend.board.service;
import com.moneybook.backend.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Collection;
import java.util.Map;
public interface BoardService {
 List<BoardCategory> categories(Authentication a);
 List<BoardCategory> adminCategories(Authentication a);
 BoardCategory createCategory(String name,String description,int order,Authentication a);
 BoardCategory updateCategory(Long uid,String name,String description,int order,boolean active,Authentication a);
 Page<BoardPost> posts(Long category,String keyword,int page,int size,Authentication a);
 Map<Long, String> authorDisplayNames(Collection<Long> authorUids);
 Map<Long, Long> commentCounts(Collection<Long> postUids);
 boolean isSuperAdmin(Authentication a);
 BoardPost createPost(Long category,String title,String content,boolean secret,Authentication a);
 BoardPost getPost(Long uid,Authentication a);
 BoardPost updatePost(Long uid,String title,String content,boolean secret,Authentication a);
 void deletePost(Long uid,Authentication a);
 BoardPost notice(Long uid,boolean notice,Authentication a);
 BoardComment createComment(Long postUid,Long parentUid,String content,boolean secret,Authentication a);
 List<BoardComment> comments(Long postUid,Authentication a);
 List<BoardComment> replies(Long commentUid,Authentication a);
 boolean canReadComment(BoardComment comment,Authentication a);
 BoardComment updateComment(Long uid,String content,boolean secret,Authentication a);
 void deleteComment(Long uid,Authentication a);
}
