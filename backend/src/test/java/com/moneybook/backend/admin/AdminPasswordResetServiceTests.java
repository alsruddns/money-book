package com.moneybook.backend.admin;

import com.moneybook.backend.admin.dto.AdminPasswordResetResponse;
import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.admin.provider.AdminAuditRecorder;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.admin.service.impl.AdminPasswordResetServiceImpl;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.recovery.RecoverySecretGenerator;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminPasswordResetServiceTests {
 @Test void superAdminGetsOneTimePasswordWhileOnlyHashAndAuditMetadataArePersisted(){
  var authorization=mock(SystemAdminAuthorizationProvider.class);var users=mock(UserRepository.class);var auths=mock(UserAuthRepository.class);
  var sessions=mock(RefreshSessionRepository.class);var audit=mock(AdminAuditRecorder.class);var encoder=new BCryptPasswordEncoder();
  var actor=mock(User.class);when(actor.getUserUid()).thenReturn(1L);when(actor.getSystemRole()).thenReturn(SystemRole.SUPER_ADMIN);
  var target=mock(User.class);when(target.getUserUid()).thenReturn(2L);when(target.getStatus()).thenReturn(UserStatus.ACTIVE);when(target.getSystemRole()).thenReturn(SystemRole.USER);
  when(authorization.requireSuperAdmin(any())).thenReturn(actor);when(users.findById(2L)).thenReturn(Optional.of(target));
  var local=UserAuth.local(target,"local-user",encoder.encode("old-password"));when(auths.findLocalByUserUidForUpdate(2L)).thenReturn(Optional.of(local));
  when(sessions.findUnrevokedByUserUid(2L)).thenReturn(List.of());
  var service=new AdminPasswordResetServiceImpl(authorization,users,auths,encoder,new RecoverySecretGenerator(),sessions,audit);

  AdminPasswordResetResponse response=service.reset(2L,null);

  assertTrue(response.passwordChangeRequired());assertTrue(response.temporaryPassword().matches("[A-HJ-NP-Z2-9]{6}(-[A-HJ-NP-Z2-9]{6}){2}"));
  assertTrue(encoder.matches(response.temporaryPassword(),local.getPasswordHash()));assertTrue(local.isPasswordChangeRequired());
  assertFalse(response.toString().contains(response.temporaryPassword()));
  verify(sessions).findUnrevokedByUserUid(2L);
  verify(audit).record(actor,AdminAuditActionType.USER_PASSWORD_RESET_BY_SUPER_ADMIN,AdminAuditTargetType.USER,2L,
          "SUPER_ADMIN이 사용자의 비밀번호를 초기화했습니다.");
 }

 @Test void superAdminCannotResetAnotherSuperAdmin(){
  var authorization=mock(SystemAdminAuthorizationProvider.class);var users=mock(UserRepository.class);
  var auths=mock(UserAuthRepository.class);var sessions=mock(RefreshSessionRepository.class);var audit=mock(AdminAuditRecorder.class);
  var actor=mock(User.class);when(actor.getUserUid()).thenReturn(1L);when(authorization.requireSuperAdmin(any())).thenReturn(actor);
  var target=mock(User.class);when(target.getSystemRole()).thenReturn(SystemRole.SUPER_ADMIN);when(users.findById(2L)).thenReturn(Optional.of(target));
  var service=new AdminPasswordResetServiceImpl(authorization,users,auths,new BCryptPasswordEncoder(),new RecoverySecretGenerator(),sessions,audit);
  assertEquals(ErrorCode.SUPER_ADMIN_MODIFICATION_FORBIDDEN,assertThrows(BusinessException.class,()->service.reset(2L,null)).getErrorCode());
  verifyNoInteractions(auths,sessions,audit);
 }
}
