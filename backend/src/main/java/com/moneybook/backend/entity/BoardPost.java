package com.moneybook.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "board_posts") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPost extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "post_uid") private Long postUid;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_uid") private BoardCategory category;
    @Column(name = "author_user_uid", nullable = false) private Long authorUserUid;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 20000) private String content;
    @Column(name = "is_secret", nullable = false) private boolean secret;
    @Column(name = "is_notice", nullable = false) private boolean notice;
    @Column(name = "view_count", nullable = false) private long viewCount;
    @Column(name = "is_deleted", nullable = false) private boolean deleted;
    public BoardPost(BoardCategory category, Long authorUid, String title, String content, boolean secret, boolean notice) {
        this.category=category; this.authorUserUid=authorUid; update(title,content,secret); this.notice=notice;
    }
    public void update(String title, String content, boolean secret) {
        if (title == null || title.isBlank() || title.trim().length() > 200 || content == null || content.isBlank() || content.trim().length() > 20000) throw new IllegalArgumentException("invalid post text");
        this.title=title.trim(); this.content=content.trim(); this.secret=secret;
    }
    public void setNotice(boolean notice) { this.notice=notice; }
    public void incrementViewCount() { this.viewCount++; }
    public void softDelete() { this.deleted=true; }
}
