package com.moneybook.backend.admin.service;
import com.moneybook.backend.admin.dto.AdminPasswordResetResponse;
import org.springframework.security.core.Authentication;
public interface AdminPasswordResetService { AdminPasswordResetResponse reset(Long userUid,Authentication authentication); }
