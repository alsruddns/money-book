package com.moneybook.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "board_comments") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardComment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "comment_uid") private Long commentUid;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="post_uid") private BoardPost post;
    @Column(name="author_user_uid", nullable=false) private Long authorUserUid;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_comment_uid") private BoardComment parent;
    @Column(nullable=false, length=5000) private String content;
    @Column(name="is_secret", nullable=false) private boolean secret;
    @Column(name="is_deleted", nullable=false) private boolean deleted;
    public BoardComment(BoardPost post, Long authorUid, BoardComment parent, String content, boolean secret) {
        this.post=post; this.authorUserUid=authorUid; this.parent=parent; update(content,secret);
    }
    public void update(String content, boolean secret) {
        if (content == null || content.isBlank() || content.trim().length() > 5000) throw new IllegalArgumentException("invalid comment text");
        this.content=content.trim(); this.secret=secret;
    }
    public void softDelete() { this.deleted=true; this.content="삭제된 댓글입니다."; this.secret=false; }
}
