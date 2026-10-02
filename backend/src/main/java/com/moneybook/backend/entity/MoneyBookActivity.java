package com.moneybook.backend.entity;

import com.moneybook.backend.enums.ActivityTargetType;
import com.moneybook.backend.enums.ActivityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 수정/삭제하지 않는 가계부 활동 스냅샷. */
@Entity
@Table(name = "money_book_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_uid")
    private Long activityUid;
    @Column(name = "money_book_uid", nullable = false)
    private Long moneyBookUid;
    @Column(name = "actor_user_uid", nullable = false)
    private Long actorUserUid;
    @Column(name = "actor_nickname", nullable = false, length = 50)
    private String actorNickname;
    @Enumerated(EnumType.STRING) @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 40)
    private ActivityTargetType targetType;
    @Column(name = "target_uid")
    private Long targetUid;
    @Column(name = "summary", nullable = false, length = 200)
    private String summary;
    @Column(name = "metadata_json", length = 500)
    private String metadataJson;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public static MoneyBookActivity create(Long bookUid, Long actorUid, String nickname, ActivityType type,
                                           ActivityTargetType targetType, Long targetUid, String summary,
                                           String metadataJson, LocalDateTime occurredAt) {
        MoneyBookActivity row = new MoneyBookActivity();
        row.moneyBookUid = bookUid; row.actorUserUid = actorUid; row.actorNickname = nickname;
        row.activityType = type; row.targetType = targetType; row.targetUid = targetUid;
        row.summary = summary; row.metadataJson = metadataJson; row.occurredAt = occurredAt;
        return row;
    }
}
