package com.moneybook.backend.board.controller;

import com.moneybook.backend.board.service.BoardService;
import com.moneybook.backend.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** 서비스 전체 공용 게시판 API를 제공하며 모든 응답의 작성자 식별값은 마스킹한다. */
@RestController @RequestMapping("/board") @RequiredArgsConstructor
public class BoardController {
 private final BoardService service;
 public record CategoryDto(Long categoryUid,String name,String description,int displayOrder,boolean active) { static CategoryDto of(BoardCategory c){return new CategoryDto(c.getCategoryUid(),c.getName(),c.getDescription(),c.getDisplayOrder(),c.isActive());} }
 public record CategoryReq(@NotBlank @Size(max=50) String name,@Size(max=300) String description,@Min(0) int displayOrder,Boolean active){}
 public record PostReq(@NotNull Long categoryUid,@NotBlank @Size(max=200) String title,@NotBlank @Size(max=20000) String content,boolean secret){}
 public record UpdatePostReq(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=20000) String content,boolean secret){}
 public record NoticeReq(boolean notice){}
 public record CommentReq(Long parentCommentUid,@NotBlank @Size(max=5000) String content,boolean secret){}
 public record UpdateCommentReq(@NotBlank @Size(max=5000) String content,boolean secret){}
 public record PostDto(Long postUid,Long categoryUid,String categoryName,String title,String content,boolean secret,boolean notice,String authorDisplayName,boolean mine,long viewCount,long commentCount,LocalDateTime regTime,LocalDateTime modTime){}
 public record CommentDto(Long commentUid,Long parentCommentUid,String content,boolean secret,String authorDisplayName,boolean deleted,LocalDateTime regTime,List<CommentDto> replies){}
 private String mask(String s){ if(s==null||s.isEmpty())return "*"; int[] c=s.codePoints().toArray(); if(c.length==1)return "*"; if(c.length==2)return new String(c,0,1)+"*"; return new String(c,0,1)+"*".repeat(c.length-2)+new String(c,c.length-1,1); }
 private PostDto dto(BoardPost p,Authentication a,boolean detail){ boolean mine=Objects.equals(p.getAuthorUserUid(),Long.valueOf(a.getName())); boolean hidden=p.isSecret()&&!mine; return new PostDto(p.getPostUid(),p.getCategory().getCategoryUid(),p.getCategory().getName(),hidden?"비밀글입니다.":p.getTitle(),detail&&!hidden?p.getContent():null,p.isSecret(),p.isNotice(),"익명",mine,p.getViewCount(),0,p.getRegTime(),p.getModTime()); }
 /** 활성 카테고리를 조회한다. 카테고리 생성과 변경은 SUPER_ADMIN 전용이다. */
 @GetMapping("/categories") public List<CategoryDto> categories(Authentication a){return service.categories(a).stream().map(CategoryDto::of).toList();}
 /** SUPER_ADMIN이 카테고리를 추가한다. */
 @PostMapping("/categories") public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryReq r,Authentication a){return ResponseEntity.status(201).body(CategoryDto.of(service.createCategory(r.name(),r.description(),r.displayOrder(),a)));}
 /** SUPER_ADMIN이 카테고리 이름, 설명, 정렬, 활성 상태를 수정한다. */
 @PatchMapping("/categories/{id}") public CategoryDto updateCategory(@PathVariable Long id,@Valid @RequestBody CategoryReq r,Authentication a){return CategoryDto.of(service.updateCategory(id,r.name(),r.description(),r.displayOrder(),Boolean.TRUE.equals(r.active()),a));}
 /** 공지 우선 최신순으로 게시글을 페이지 조회한다. 비밀글 keyword 본문 검색은 제외한다. */
 @GetMapping("/posts") public Page<PostDto> posts(@RequestParam(required=false) Long categoryUid,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,Authentication a){return service.posts(categoryUid,keyword,page,size,a).map(p->dto(p,a,false));}
 /** 활성 카테고리에 게시글을 등록한다. 공지 상태는 일반 등록 DTO에서 설정할 수 없다. */
 @PostMapping("/posts") public ResponseEntity<PostDto> create(@Valid @RequestBody PostReq r,Authentication a){return ResponseEntity.status(201).body(dto(service.createPost(r.categoryUid(),r.title(),r.content(),r.secret(),a),a,true));}
 /** 게시글 상세를 조회한다. 비밀글은 작성자와 SUPER_ADMIN만 열람할 수 있다. */
 @GetMapping("/posts/{id}") public PostDto detail(@PathVariable Long id,Authentication a){return dto(service.getPost(id,a),a,true);}
 /** 작성자가 자신의 게시글을 수정한다. */
 @PatchMapping("/posts/{id}") public PostDto update(@PathVariable Long id,@Valid @RequestBody UpdatePostReq r,Authentication a){return dto(service.updatePost(id,r.title(),r.content(),r.secret(),a),a,true);}
 /** 작성자 또는 SUPER_ADMIN이 게시글을 soft delete한다. */
 @DeleteMapping("/posts/{id}") public ResponseEntity<Void> delete(@PathVariable Long id,Authentication a){service.deletePost(id,a);return ResponseEntity.noContent().build();}
 /** SUPER_ADMIN이 게시글 공지 상태를 변경한다. */
 @PatchMapping("/posts/{id}/notice") public PostDto notice(@PathVariable Long id,@RequestBody NoticeReq r,Authentication a){return dto(service.notice(id,r.notice(),a),a,true);}
 /** 게시글에 댓글 또는 한 단계 대댓글을 작성한다. */
 @PostMapping("/posts/{id}/comments") public ResponseEntity<CommentDto> createComment(@PathVariable Long id,@Valid @RequestBody CommentReq r,Authentication a){return ResponseEntity.status(201).body(commentDto(service.createComment(id,r.parentCommentUid(),r.content(),r.secret(),a),a));}
 /** 게시글 댓글과 대댓글을 조회하며 열람 불가 비밀 댓글의 본문은 전달하지 않는다. */
 @GetMapping("/posts/{id}/comments") public List<CommentDto> comments(@PathVariable Long id,Authentication a){return service.comments(id,a).stream().map(c->commentDto(c,a)).toList();}
 /** 댓글 작성자가 댓글을 수정한다. */
 @PatchMapping("/comments/{id}") public CommentDto updateComment(@PathVariable Long id,@Valid @RequestBody UpdateCommentReq r,Authentication a){return commentDto(service.updateComment(id,r.content(),r.secret(),a),a);}
 /** 작성자 또는 SUPER_ADMIN이 댓글을 soft delete한다. */
 @DeleteMapping("/comments/{id}") public ResponseEntity<Void> deleteComment(@PathVariable Long id,Authentication a){service.deleteComment(id,a);return ResponseEntity.noContent().build();}
 private CommentDto commentDto(BoardComment c,Authentication a){boolean hidden=c.isSecret()&&!service.canReadComment(c,a); List<CommentDto> replies=c.getParent()==null?service.replies(c.getCommentUid(),a).stream().map(x->commentDto(x,a)).toList():List.of(); return new CommentDto(c.getCommentUid(),c.getParent()==null?null:c.getParent().getCommentUid(),hidden?"비밀 댓글입니다.":c.getContent(),c.isSecret(),"익명",c.isDeleted(),c.getRegTime(),replies);}
}
