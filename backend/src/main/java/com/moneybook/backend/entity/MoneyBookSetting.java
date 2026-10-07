package com.moneybook.backend.entity;

import com.moneybook.backend.enums.WeekStartDay;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_settings", uniqueConstraints =
        @UniqueConstraint(name = "uq_money_book_settings_book", columnNames = "money_book_uid"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookSetting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "money_book_setting_uid")
    private Long moneyBookSettingUid;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_start_day", nullable = false, length = 10)
    private WeekStartDay weekStartDay;

    private MoneyBookSetting(MoneyBook moneyBook, WeekStartDay weekStartDay) {
        this.moneyBook = Objects.requireNonNull(moneyBook);
        changeWeekStartDay(weekStartDay);
    }

    public static MoneyBookSetting create(MoneyBook moneyBook, WeekStartDay weekStartDay) {
        return new MoneyBookSetting(moneyBook, weekStartDay);
    }

    public void changeWeekStartDay(WeekStartDay weekStartDay) {
        this.weekStartDay = Objects.requireNonNull(weekStartDay);
    }
}
