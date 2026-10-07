package com.moneybook.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "board_categories") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardCategory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "category_uid") private Long categoryUid;
    @Column(nullable = false, length = 50, unique = true) private String name;
    @Column(length = 300) private String description;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "is_deleted", nullable = false) private boolean deleted;
    public BoardCategory(String name, String description, int displayOrder) { change(name, description, displayOrder, true); }
    public void change(String name, String description, int displayOrder, boolean active) {
        if (name == null || name.isBlank() || name.trim().length() > 50) throw new IllegalArgumentException("invalid category name");
        this.name = name.trim(); this.description = description == null ? null : description.trim();
        this.displayOrder = displayOrder; this.active = active;
    }
    public void softDelete() { this.deleted = true; this.active = false; }
}
