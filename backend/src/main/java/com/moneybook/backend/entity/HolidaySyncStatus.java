package com.moneybook.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "holiday_sync_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HolidaySyncStatus {
    @Id
    @Column(name = "\"year\"")
    private int year;

    @Column(name = "last_attempt_at", nullable = false)
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    private HolidaySyncStatus(int year, LocalDateTime lastAttemptAt) {
        this.year = year;
        this.lastAttemptAt = Objects.requireNonNull(lastAttemptAt, "lastAttemptAt");
    }

    public static HolidaySyncStatus attempted(int year, LocalDateTime time) {
        return new HolidaySyncStatus(year, time);
    }

    public void markAttempted(LocalDateTime time) {
        this.lastAttemptAt = Objects.requireNonNull(time, "time");
    }

    public void markSynced(LocalDateTime time) {
        this.lastSyncedAt = Objects.requireNonNull(time, "time");
    }
}
