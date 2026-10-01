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

import java.util.Objects;

@Getter
@Entity
@Table(name = "money_books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "money_book_uid")
    private Long moneyBookUid;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_user_uid", nullable = false)
    private Long ownerUserUid;

    private MoneyBook(String name, Long ownerUserUid) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("name must contain 1 to 100 characters");
        }
        this.name = name;
        this.ownerUserUid = Objects.requireNonNull(ownerUserUid, "ownerUserUid");
    }

    public static MoneyBook create(String name, Long ownerUserUid) {
        return new MoneyBook(name, ownerUserUid);
    }
}
