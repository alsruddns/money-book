package com.moneybook.backend.entity;

import com.moneybook.backend.enums.InvitationStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "money_book_users", uniqueConstraints =
        @UniqueConstraint(name = "uq_money_book_users_book_user", columnNames = {"money_book_uid", "user_uid"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoneyBookUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "money_book_user_uid")
    private Long moneyBookUserUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "money_book_uid", nullable = false)
    private MoneyBook moneyBook;

    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate;

    @Column(name = "can_read", nullable = false)
    private boolean canRead;

    @Column(name = "can_update", nullable = false)
    private boolean canUpdate;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 30)
    private InvitationStatus invitationStatus;

    private MoneyBookUser(MoneyBook moneyBook, Long userUid, InvitationStatus invitationStatus,
                          boolean admin, boolean canCreate, boolean canRead,
                          boolean canUpdate, boolean canDelete) {
        this.moneyBook = Objects.requireNonNull(moneyBook, "moneyBook");
        this.userUid = Objects.requireNonNull(userUid, "userUid");
        this.invitationStatus = Objects.requireNonNull(invitationStatus, "invitationStatus");
        applyPermissions(admin, canCreate, canRead, canUpdate, canDelete);
    }

    public static MoneyBookUser owner(MoneyBook moneyBook, Long userUid) {
        if (!Objects.equals(moneyBook.getOwnerUserUid(), userUid)) {
            throw new IllegalArgumentException("owner membership must match money book owner");
        }
        return new MoneyBookUser(moneyBook, userUid, InvitationStatus.ACCEPTED,
                true, true, true, true, true);
    }

    public static MoneyBookUser invite(MoneyBook moneyBook, Long userUid,
                                       boolean admin, boolean canCreate, boolean canRead,
                                       boolean canUpdate, boolean canDelete) {
        return new MoneyBookUser(moneyBook, userUid, InvitationStatus.PENDING,
                admin, canCreate, canRead, canUpdate, canDelete);
    }

    public void reinvite(boolean admin, boolean canCreate, boolean canRead,
                         boolean canUpdate, boolean canDelete) {
        if (invitationStatus != InvitationStatus.REJECTED) {
            throw new IllegalStateException("Only rejected invitations can be renewed");
        }
        applyPermissions(admin, canCreate, canRead, canUpdate, canDelete);
        invitationStatus = InvitationStatus.PENDING;
    }

    public void acceptInvitation() {
        requirePending();
        invitationStatus = InvitationStatus.ACCEPTED;
    }

    public void rejectInvitation() {
        requirePending();
        invitationStatus = InvitationStatus.REJECTED;
    }

    private void requirePending() {
        if (invitationStatus != InvitationStatus.PENDING) {
            throw new IllegalStateException("Only pending invitations can be answered");
        }
    }

    private void applyPermissions(boolean admin, boolean canCreate, boolean canRead,
                                  boolean canUpdate, boolean canDelete) {
        this.admin = admin;
        this.canCreate = admin || canCreate;
        this.canRead = admin || canRead;
        this.canUpdate = admin || canUpdate;
        this.canDelete = admin || canDelete;
    }
}
