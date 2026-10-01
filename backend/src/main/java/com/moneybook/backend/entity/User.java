package com.moneybook.backend.entity;

import com.moneybook.backend.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_uid")
    private Long userUid;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    private User(String nickname, String profileImageUrl) {
        this.nickname = requireText(nickname, "nickname");
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE;
    }

    public static User create(String nickname, String profileImageUrl) {
        return new User(nickname, profileImageUrl);
    }

    public void changeNickname(String nickname) {
        this.nickname = requireText(nickname, "nickname");
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void changeStatus(UserStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
