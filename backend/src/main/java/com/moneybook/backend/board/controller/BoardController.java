package com.moneybook.backend.board.controller;

import com.moneybook.backend.board.service.BoardService;
import com.moneybook.backend.entity.BoardCategory;
import com.moneybook.backend.entity.BoardComment;
import com.moneybook.backend.entity.BoardPost;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 서비스 전체 공용 게시판과 최고 관리자 카테고리 API를 제공한다. */
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService service;

    public record CategoryDto(Long categoryUid, String name, String description, int displayOrder, boolean active) {
        static CategoryDto of(BoardCategory category) {
            return new CategoryDto(category.getCategoryUid(), category.getName(), category.getDescription(),
                    category.getDisplayOrder(), category.isActive());
        }
    }

    public record CategoryReq(@NotBlank @Size(max = 50) String name, @Size(max = 300) String description,
                              @Min(0) int displayOrder, Boolean active) { }
    public record PostReq(@NotNull Long categoryUid, @NotBlank @Size(max = 200) String title,
                          @NotBlank @Size(max = 20000) String content, boolean secret) { }
    public record UpdatePostReq(@NotBlank @Size(max = 200) String title,
                                @NotBlank @Size(max = 20000) String content, boolean secret) { }
    public record NoticeReq(boolean notice) { }
    public record CommentReq(Long parentCommentUid, @NotBlank @Size(max = 5000) String content, boolean secret) { }
    public record UpdateCommentReq(@NotBlank @Size(max = 5000) String content, boolean secret) { }
    public record PostDto(Long postUid, Long categoryUid, String categoryName, String title, String content,
                          boolean secret, boolean notice, String authorDisplayName, boolean mine,
                          boolean canEdit, boolean canDelete, long viewCount, long commentCount,
                          LocalDateTime regTime, LocalDateTime modTime) { }
    public record CommentDto(Long commentUid, Long parentCommentUid, String content, boolean secret,
                             String authorDisplayName, boolean deleted, LocalDateTime regTime,
                             boolean canEdit, boolean canDelete, List<CommentDto> replies) { }

    /** 활성 카테고리를 조회하며, SUPER_ADMIN만 includeInactive=true로 비활성 카테고리도 조회한다. */
    @GetMapping("/categories")
    public List<CategoryDto> categories(@RequestParam(defaultValue = "false") boolean includeInactive,
                                        Authentication authentication) {
        List<BoardCategory> result = includeInactive
                ? service.adminCategories(authentication) : service.categories(authentication);
        return result.stream().map(CategoryDto::of).toList();
    }

    /** SUPER_ADMIN만 카테고리를 생성할 수 있다. */
    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryReq request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryDto.of(service.createCategory(
                request.name(), request.description(), request.displayOrder(), authentication)));
    }

    /** SUPER_ADMIN만 이름, 설명, 정렬 순서와 활성 상태를 수정할 수 있다. */
    @PatchMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryReq request,
                                      Authentication authentication) {
        return CategoryDto.of(service.updateCategory(id, request.name(), request.description(),
                request.displayOrder(), Boolean.TRUE.equals(request.active()), authentication));
    }

    /** 게시글을 공지 우선, 최신순으로 조회한다. page는 0부터 시작한다. */
    @GetMapping("/posts")
    public Page<PostDto> posts(@RequestParam(required = false) Long categoryUid,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Authentication authentication) {
        Page<BoardPost> result = service.posts(categoryUid, keyword, page, size, authentication);
        Map<Long, String> authors = service.authorDisplayNames(
                result.getContent().stream().map(BoardPost::getAuthorUserUid).toList());
        Map<Long, Long> commentCounts = service.commentCounts(
                result.getContent().stream().map(BoardPost::getPostUid).toList());
        boolean superAdmin = service.isSuperAdmin(authentication);
        Long viewerUid = userUid(authentication);
        return result.map(post -> postDto(post, authors, commentCounts.getOrDefault(post.getPostUid(), 0L),
                viewerUid, superAdmin, false));
    }

    /** 활성 카테고리에 새 게시글을 등록한다. 공지 상태는 별도 SUPER_ADMIN API로만 변경한다. */
    @PostMapping("/posts")
    public ResponseEntity<PostDto> create(@Valid @RequestBody PostReq request, Authentication authentication) {
        BoardPost post = service.createPost(request.categoryUid(), request.title(), request.content(),
                request.secret(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(singlePost(post, authentication, true));
    }

    /** 게시글 상세를 조회한다. 비밀글은 작성자와 SUPER_ADMIN만 확인할 수 있다. */
    @GetMapping("/posts/{id}")
    public PostDto detail(@PathVariable Long id, Authentication authentication) {
        return singlePost(service.getPost(id, authentication), authentication, true);
    }

    /** 작성자 본인이 게시글 제목, 내용과 비밀글 여부를 수정한다. */
    @PatchMapping("/posts/{id}")
    public PostDto update(@PathVariable Long id, @Valid @RequestBody UpdatePostReq request,
                          Authentication authentication) {
        return singlePost(service.updatePost(id, request.title(), request.content(), request.secret(), authentication),
                authentication, true);
    }

    /** 작성자 또는 SUPER_ADMIN이 게시글을 soft delete한다. */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        service.deletePost(id, authentication);
        return ResponseEntity.noContent().build();
    }

    /** SUPER_ADMIN 전용 공지 상태 변경 API이며 일반 게시글 수정 계약과 분리한다. */
    @PatchMapping("/posts/{id}/notice")
    public PostDto notice(@PathVariable Long id, @RequestBody NoticeReq request, Authentication authentication) {
        return singlePost(service.notice(id, request.notice(), authentication), authentication, true);
    }

    /** 게시글에 댓글 또는 한 단계 대댓글을 등록한다. */
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentDto> createComment(@PathVariable Long id, @Valid @RequestBody CommentReq request,
                                                     Authentication authentication) {
        BoardComment comment = service.createComment(id, request.parentCommentUid(), request.content(),
                request.secret(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(singleComment(comment, authentication));
    }

    /** 댓글과 대댓글을 조회하고, 권한 없는 비밀 댓글의 content는 placeholder로 반환한다. */
    @GetMapping("/posts/{id}/comments")
    public List<CommentDto> comments(@PathVariable Long id, Authentication authentication) {
        List<BoardComment> rows = service.comments(id, authentication);
        Long viewerUid = userUid(authentication);
        boolean superAdmin = service.isSuperAdmin(authentication);
        Map<Long, String> authors = service.authorDisplayNames(rows.stream().map(BoardComment::getAuthorUserUid).toList());
        Map<Long, List<BoardComment>> children = rows.stream().filter(row -> row.getParent() != null)
                .collect(Collectors.groupingBy(row -> row.getParent().getCommentUid()));
        return rows.stream().filter(row -> row.getParent() == null)
                .map(row -> commentDto(row, children, authors, viewerUid, superAdmin)).toList();
    }

    /** 작성자 본인이 댓글 또는 대댓글을 수정한다. */
    @PatchMapping("/comments/{id}")
    public CommentDto updateComment(@PathVariable Long id, @Valid @RequestBody UpdateCommentReq request,
                                    Authentication authentication) {
        return singleComment(service.updateComment(id, request.content(), request.secret(), authentication), authentication);
    }

    /** 작성자 또는 SUPER_ADMIN이 댓글 또는 대댓글을 soft delete한다. */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
        service.deleteComment(id, authentication);
        return ResponseEntity.noContent().build();
    }

    private PostDto singlePost(BoardPost post, Authentication authentication, boolean detail) {
        Long viewerUid = userUid(authentication);
        Map<Long, String> authors = service.authorDisplayNames(List.of(post.getAuthorUserUid()));
        long count = service.commentCounts(List.of(post.getPostUid())).getOrDefault(post.getPostUid(), 0L);
        return postDto(post, authors, count, viewerUid, service.isSuperAdmin(authentication), detail);
    }

    private PostDto postDto(BoardPost post, Map<Long, String> authors, long commentCount, Long viewerUid,
                            boolean superAdmin, boolean detail) {
        boolean mine = Objects.equals(post.getAuthorUserUid(), viewerUid);
        boolean hideTitle = post.isSecret() && !mine && !superAdmin;
        return new PostDto(post.getPostUid(), post.getCategory().getCategoryUid(), post.getCategory().getName(),
                hideTitle ? "비밀글입니다." : post.getTitle(), detail && !hideTitle ? post.getContent() : null,
                post.isSecret(), post.isNotice(), authors.getOrDefault(post.getAuthorUserUid(), "*"), mine,
                mine, mine || superAdmin, post.getViewCount(), commentCount, post.getRegTime(), post.getModTime());
    }

    private CommentDto singleComment(BoardComment comment, Authentication authentication) {
        Long viewerUid = userUid(authentication);
        Map<Long, String> authors = service.authorDisplayNames(List.of(comment.getAuthorUserUid()));
        return commentDto(comment, Map.of(), authors, viewerUid, service.isSuperAdmin(authentication));
    }

    private CommentDto commentDto(BoardComment comment, Map<Long, List<BoardComment>> children,
                                  Map<Long, String> authors, Long viewerUid, boolean superAdmin) {
        boolean mine = Objects.equals(comment.getAuthorUserUid(), viewerUid);
        boolean hideContent = comment.isSecret() && !mine && !superAdmin;
        List<CommentDto> replies = children.getOrDefault(comment.getCommentUid(), List.of()).stream()
                .map(reply -> commentDto(reply, children, authors, viewerUid, superAdmin)).toList();
        return new CommentDto(comment.getCommentUid(), comment.getParent() == null ? null : comment.getParent().getCommentUid(),
                hideContent ? "비밀 댓글입니다." : comment.getContent(), comment.isSecret(),
                authors.getOrDefault(comment.getAuthorUserUid(), "*"), comment.isDeleted(), comment.getRegTime(),
                mine && !comment.isDeleted(), (mine || superAdmin) && !comment.isDeleted(), replies);
    }

    private Long userUid(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
