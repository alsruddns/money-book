package com.moneybook.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedBy
    @Column(name = "reg_r_id")
    private Long regRId;

    @CreatedDate
    @Column(name = "reg_time", nullable = false, updatable = false)
    private LocalDateTime regTime;

    @LastModifiedBy
    @Column(name = "mod_r_id")
    private Long modRId;

    @LastModifiedDate
    @Column(name = "mod_time", nullable = false)
    private LocalDateTime modTime;
}
