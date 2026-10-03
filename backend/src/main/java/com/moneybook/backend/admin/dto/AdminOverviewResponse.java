package com.moneybook.backend.admin.dto;

import java.math.BigDecimal;

public record AdminOverviewResponse(long totalUsers, long activeUsers, long suspendedUsers,
        long systemAdminCount, long superAdminCount, long totalMoneyBooks, long activitiesToday,
        long blockedUsers, long withdrawnUsers, long totalMemberships, long activeMemberCount,
        BigDecimal averageMembersPerMoneyBook, long todayNewUsers, long last7DaysNewUsers,
        long last30DaysNewUsers, long todayNewMoneyBooks, long last7DaysNewMoneyBooks,
        long last30DaysNewMoneyBooks, long last7DaysActivityCount, long last30DaysActivityCount,
        long activeSessionCount, long recentAdminAuditCount, long todayActivityCount) { }
