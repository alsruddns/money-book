package com.moneybook.backend.admin.repository;

import com.moneybook.backend.admin.dto.*;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminReadRepository {
    Page<AdminUserResponse> users(String keyword, UserStatus status, SystemRole role, Pageable pageable);
    Optional<AdminUserDetailResponse> user(Long uid);
    Page<AdminMoneyBookResponse> moneyBooks(String keyword, Long ownerUid, Pageable pageable);
    Optional<AdminMoneyBookDetailResponse> moneyBook(Long uid);
    List<Object[]> moneyBookNames(List<Long> uids);
    long countActivitiesBetween(LocalDateTime start, LocalDateTime end);
    AdminOverviewResponse overview();
}
