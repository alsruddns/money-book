package com.moneybook.backend.admin.service.impl;
import com.moneybook.backend.admin.dto.AdminPasswordResetResponse;
import com.moneybook.backend.admin.enums.*;
import com.moneybook.backend.admin.provider.*;
import com.moneybook.backend.admin.service.AdminPasswordResetService;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.*;
import com.moneybook.backend.enums.*;
import com.moneybook.backend.recovery.RecoverySecretGenerator;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
@Service @RequiredArgsConstructor
public class AdminPasswordResetServiceImpl implements AdminPasswordResetService {
 private final SystemAdminAuthorizationProvider authorization;private final UserRepository users;private final UserAuthRepository auths;
 private final PasswordEncoder encoder;private final RecoverySecretGenerator secrets;private final RefreshSessionRepository sessions;private final AdminAuditRecorder audit;
 /** Only a SUPER_ADMIN may reset another non-SUPER_ADMIN LOCAL user's password. */
 @Override @Transactional public AdminPasswordResetResponse reset(Long uid,Authentication authentication){var actor=authorization.requireSuperAdmin(authentication);
  if(actor.getUserUid().equals(uid))throw new BusinessException(ErrorCode.SELF_ADMIN_MODIFICATION_FORBIDDEN);
  var target=users.findById(uid).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
  if(target.getSystemRole()==SystemRole.SUPER_ADMIN)throw new BusinessException(ErrorCode.SUPER_ADMIN_MODIFICATION_FORBIDDEN);
  if(target.getStatus()!=UserStatus.ACTIVE)throw new BusinessException(ErrorCode.USER_INACTIVE);
  var local=auths.findLocalByUserUidForUpdate(uid).orElseThrow(()->new BusinessException(ErrorCode.LOCAL_AUTH_NOT_FOUND));
  String temporary=secrets.newTemporaryPassword();local.changePasswordHash(encoder.encode(temporary));local.requirePasswordChange();auths.save(local);
  LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);sessions.findUnrevokedByUserUid(uid).forEach(s->s.revoke(now,"ADMIN_PASSWORD_RESET"));
  audit.record(actor,AdminAuditActionType.USER_PASSWORD_RESET_BY_SUPER_ADMIN,AdminAuditTargetType.USER,uid,"SUPER_ADMIN이 사용자의 비밀번호를 초기화했습니다.");
  return new AdminPasswordResetResponse(uid,temporary,true);
 }
}
