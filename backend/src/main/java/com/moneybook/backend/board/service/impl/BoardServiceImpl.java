package com.moneybook.backend.board.service.impl;

import com.moneybook.backend.admin.enums.*;
import com.moneybook.backend.admin.provider.AdminAuditRecorder;
import com.moneybook.backend.board.repository.impl.*;
import com.moneybook.backend.board.service.BoardService;
import com.moneybook.backend.common.exception.*;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.*;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
 private final BoardCategoryJpaRepository categories; private final BoardPostJpaRepository posts;
 private final BoardCommentJpaRepository comments; private final UserRepository users; private final AdminAuditRecorder audit;
 private User actor(Authentication a) { try { return users.findById(Long.valueOf(a.getName())).filter(u->u.getStatus()==UserStatus.ACTIVE).orElseThrow(()->new BusinessException(ErrorCode.BOARD_FORBIDDEN)); } catch(NumberFormatException e){ throw new BusinessException(ErrorCode.BOARD_FORBIDDEN); } }
 private User superAdmin(Authentication a) { User u=actor(a); if(u.getSystemRole()!=SystemRole.SUPER_ADMIN) throw new BusinessException(ErrorCode.BOARD_FORBIDDEN); return u; }
 private BoardPost post(Long id) { return posts.findById(id).filter(p->!p.isDeleted()).orElseThrow(()->new BusinessException(ErrorCode.BOARD_POST_NOT_FOUND)); }
 @Override @Transactional(readOnly=true) public List<BoardCategory> categories(Authentication a){ actor(a); return categories.findByDeletedFalseAndActiveTrueOrderByDisplayOrderAscCategoryUidAsc(); }
 @Override @Transactional public BoardCategory createCategory(String n,String d,int o,Authentication a){ User u=superAdmin(a); BoardCategory c=categories.save(new BoardCategory(n,d,o)); audit.record(u,AdminAuditActionType.BOARD_CATEGORY_CREATED,AdminAuditTargetType.BOARD_CATEGORY,c.getCategoryUid(),"게시판 카테고리를 등록했습니다."); return c; }
 @Override @Transactional public BoardCategory updateCategory(Long id,String n,String d,int o,boolean active,Authentication a){ User u=superAdmin(a); BoardCategory c=categories.findById(id).filter(x->!x.isDeleted()).orElseThrow(()->new BusinessException(ErrorCode.BOARD_CATEGORY_NOT_FOUND)); c.change(n,d,o,active); audit.record(u,AdminAuditActionType.BOARD_CATEGORY_UPDATED,AdminAuditTargetType.BOARD_CATEGORY,id,"게시판 카테고리를 수정했습니다."); return c; }
 @Override @Transactional(readOnly=true) public Page<BoardPost> posts(Long category,String keyword,int page,int size,Authentication a){ actor(a); if(page<0||size<1||size>100) throw new BusinessException(ErrorCode.VALIDATION_FAILED); String q=keyword==null||keyword.isBlank()?null:keyword.trim(); return posts.search(category,q,PageRequest.of(page,size,Sort.by(Sort.Order.desc("notice"),Sort.Order.desc("regTime"),Sort.Order.desc("postUid")))); }
 @Override @Transactional public BoardPost createPost(Long category,String title,String content,boolean secret,Authentication a){ User u=actor(a); BoardCategory c=categories.findById(category).filter(x->!x.isDeleted()&&x.isActive()).orElseThrow(()->new BusinessException(ErrorCode.BOARD_CATEGORY_NOT_FOUND)); return posts.save(new BoardPost(c,u.getUserUid(),title,content,secret,false)); }
 @Override @Transactional public BoardPost getPost(Long id,Authentication a){ User u=actor(a); BoardPost p=post(id); if(p.isSecret()&&!Objects.equals(p.getAuthorUserUid(),u.getUserUid())&&u.getSystemRole()!=SystemRole.SUPER_ADMIN) throw new BusinessException(ErrorCode.BOARD_SECRET_ACCESS_DENIED); p.incrementViewCount(); return p; }
 @Override @Transactional public BoardPost updatePost(Long id,String title,String content,boolean secret,Authentication a){ User u=actor(a); BoardPost p=post(id); if(!Objects.equals(p.getAuthorUserUid(),u.getUserUid())) throw new BusinessException(ErrorCode.BOARD_FORBIDDEN); p.update(title,content,secret); return p; }
 @Override @Transactional public void deletePost(Long id,Authentication a){ User u=actor(a); BoardPost p=post(id); if(!Objects.equals(p.getAuthorUserUid(),u.getUserUid())&&u.getSystemRole()!=SystemRole.SUPER_ADMIN) throw new BusinessException(ErrorCode.BOARD_FORBIDDEN); p.softDelete(); if(!Objects.equals(p.getAuthorUserUid(),u.getUserUid())) audit.record(u,AdminAuditActionType.BOARD_POST_MODERATED,AdminAuditTargetType.BOARD_POST,id,"게시글을 운영 조치로 삭제했습니다."); }
 @Override @Transactional public BoardPost notice(Long id,boolean value,Authentication a){ User u=superAdmin(a); BoardPost p=post(id); p.setNotice(value); audit.record(u,AdminAuditActionType.BOARD_NOTICE_CHANGED,AdminAuditTargetType.BOARD_POST,id,"게시글 공지 상태를 변경했습니다."); return p; }
 @Override @Transactional public BoardComment createComment(Long postUid,Long parentUid,String content,boolean secret,Authentication a){ User u=actor(a); BoardPost p=post(postUid); if(p.isSecret()&&!Objects.equals(p.getAuthorUserUid(),u.getUserUid())&&u.getSystemRole()!=SystemRole.SUPER_ADMIN) throw new BusinessException(ErrorCode.BOARD_SECRET_ACCESS_DENIED); BoardComment parent=null; if(parentUid!=null){ parent=comments.findById(parentUid).filter(c->!c.isDeleted()&&c.getPost().getPostUid().equals(postUid)).orElseThrow(()->new BusinessException(ErrorCode.BOARD_COMMENT_NOT_FOUND)); if(parent.getParent()!=null) throw new BusinessException(ErrorCode.BOARD_INVALID_PARENT_COMMENT); } return comments.save(new BoardComment(p,u.getUserUid(),parent,content,secret)); }
 @Override @Transactional(readOnly=true) public List<BoardComment> comments(Long postUid,Authentication a){ User u=actor(a); BoardPost p=post(postUid); if(p.isSecret()&&!Objects.equals(p.getAuthorUserUid(),u.getUserUid())&&u.getSystemRole()!=SystemRole.SUPER_ADMIN) throw new BusinessException(ErrorCode.BOARD_SECRET_ACCESS_DENIED); return comments.findByPost_PostUidAndParentIsNullOrderByRegTimeAscCommentUidAsc(postUid); }
 @Override @Transactional(readOnly=true) public List<BoardComment> replies(Long commentUid,Authentication a){ actor(a); return comments.findByParent_CommentUidOrderByRegTimeAscCommentUidAsc(commentUid); }
 @Override @Transactional(readOnly=true) public boolean canReadComment(BoardComment c,Authentication a){ User u=actor(a); return !c.isSecret()||Objects.equals(c.getAuthorUserUid(),u.getUserUid())||u.getSystemRole()==SystemRole.SUPER_ADMIN; }
 private boolean canSee(BoardComment c,User u){return !c.isSecret()||Objects.equals(c.getAuthorUserUid(),u.getUserUid())||u.getSystemRole()==SystemRole.SUPER_ADMIN;}
 @Override @Transactional public BoardComment updateComment(Long id,String content,boolean secret,Authentication a){ User u=actor(a); BoardComment c=comments.findById(id).filter(x->!x.isDeleted()).orElseThrow(()->new BusinessException(ErrorCode.BOARD_COMMENT_NOT_FOUND)); if(!Objects.equals(c.getAuthorUserUid(),u.getUserUid())) throw new BusinessException(ErrorCode.BOARD_FORBIDDEN); c.update(content,secret); return c; }
 @Override @Transactional public void deleteComment(Long id,Authentication a){ User u=actor(a); BoardComment c=comments.findById(id).filter(x->!x.isDeleted()).orElseThrow(()->new BusinessException(ErrorCode.BOARD_COMMENT_NOT_FOUND)); if(!Objects.equals(c.getAuthorUserUid(),u.getUserUid())&&u.getSystemRole()!=SystemRole.SUPER_ADMIN) throw new BusinessException(ErrorCode.BOARD_FORBIDDEN); c.softDelete(); if(!Objects.equals(c.getAuthorUserUid(),u.getUserUid())) audit.record(u,AdminAuditActionType.BOARD_COMMENT_MODERATED,AdminAuditTargetType.BOARD_COMMENT,id,"댓글을 운영 조치로 삭제했습니다."); }
}
