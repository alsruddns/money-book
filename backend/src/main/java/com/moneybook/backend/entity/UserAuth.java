package com.moneybook.backend.entity;

import com.moneybook.backend.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "user_auth")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_auth_uid")
    private Long userAuthUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_uid", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private AuthProvider provider;

    @Column(name = "login_id", length = 100)
    private String loginId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(name = "security_question_code", length = 40)
    private String securityQuestionCode;

    @Column(name = "security_answer_hash", length = 100)
    private String securityAnswerHash;

    @Column(name = "verified_email", length = 254)
    private String verifiedEmail;

    @Column(name = "email_verified_at")
    private java.time.LocalDateTime emailVerifiedAt;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    private UserAuth(User user, AuthProvider provider, String loginId,
                     String passwordHash, String providerUserId) {
        this.user = Objects.requireNonNull(user, "user");
        this.provider = provider;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.providerUserId = providerUserId;
    }

    /** Stores only an encoded password hash supplied by the future authentication service. */
    public static UserAuth local(User user, String loginId, String passwordHash) {
        return new UserAuth(user, AuthProvider.LOCAL,
                requireText(loginId, "loginId"), requireText(passwordHash, "passwordHash"), null);
    }

    public static UserAuth oauth(User user, AuthProvider provider, String providerUserId) {
        if (provider == null || provider == AuthProvider.LOCAL) {
            throw new IllegalArgumentException("An OAuth provider is required");
        }
        return new UserAuth(user, provider, null, null,
                requireText(providerUserId, "providerUserId"));
    }

    /** Accepts only an encoded password hash from the future authentication service. */
    public void changePasswordHash(String passwordHash) {
        if (provider != AuthProvider.LOCAL) {
            throw new IllegalStateException("Only LOCAL authentication has a password hash");
        }
        this.passwordHash = requireText(passwordHash, "passwordHash");
    }

    public void changeSecurityQuestion(String code, String answerHash) {
        requireLocal();
        this.securityQuestionCode = requireText(code, "securityQuestionCode");
        this.securityAnswerHash = requireText(answerHash, "securityAnswerHash");
    }

    public void changeVerifiedEmail(String email, java.time.LocalDateTime verifiedAt) {
        requireLocal();
        this.verifiedEmail = email;
        this.emailVerifiedAt = email == null ? null : Objects.requireNonNull(verifiedAt, "verifiedAt");
    }

    public void requirePasswordChange() { requireLocal(); this.passwordChangeRequired = true; }

    public void completePasswordChange(String encodedPassword) {
        changePasswordHash(encodedPassword);
        this.passwordChangeRequired = false;
    }

    private void requireLocal() {
        if (provider != AuthProvider.LOCAL) throw new IllegalStateException("Only LOCAL authentication has recovery settings");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
