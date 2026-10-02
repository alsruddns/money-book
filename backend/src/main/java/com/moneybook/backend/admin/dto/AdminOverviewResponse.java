package com.moneybook.backend.admin.dto;

public record AdminOverviewResponse(long totalUsers, long activeUsers, long suspendedUsers,
        long systemAdminCount, long superAdminCount, long totalMoneyBooks, long activitiesToday) { }
