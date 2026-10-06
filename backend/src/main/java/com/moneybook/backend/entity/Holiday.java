package com.moneybook.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "holidays")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holiday extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_uid")
    private Long holidayUid;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "holiday_type", length = 30)
    private String holidayType;

    @Column(name = "is_holiday", nullable = false)
    private boolean holiday;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    private Holiday(LocalDate holidayDate, String name, String holidayType) {
        this.holidayDate = Objects.requireNonNull(holidayDate, "holidayDate");
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Invalid holiday name");
        }
        this.name = name;
        this.holidayType = holidayType;
        this.holiday = true;
        this.source = "KASI";
    }

    public static Holiday fromKasi(LocalDate holidayDate, String name, String holidayType) {
        return new Holiday(holidayDate, name, holidayType);
    }
}
