package com.moneybook.backend.admin;

import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.admin.dto.AdminUserDetailResponse;
import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.admin.provider.AdminAuditRecorder;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.admin.repository.AdminReadRepository;
import com.moneybook.backend.admin.service.impl.AdminServiceImpl;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServicePolicyTests {
    private final SystemAdminAuthorizationProvider authorization=mock(SystemAdminAuthorizationProvider.class);
    private final AdminReadRepository reads=mock(AdminReadRepository.class);
    private final AdminAuditRepository audits=mock(AdminAuditRepository.class);
    private final ActivityRepository activities=mock(ActivityRepository.class);
    private final UserRepository users=mock(UserRepository.class);
    private final AdminAuditRecorder recorder=mock(AdminAuditRecorder.class);
    private final AdminServiceImpl service=new AdminServiceImpl(authorization,reads,audits,activities,users,recorder);
    private final Authentication auth=mock(Authentication.class);

    @Test void superAdminCanGrantAndRevokeSystemAdminButCannotGrantSuperOrChangeSelf() {
        User actor=user(1,SystemRole.SUPER_ADMIN);
        User target=user(2,SystemRole.USER);
        when(authorization.requireSuperAdmin(auth)).thenReturn(actor);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(reads.user(2L)).thenReturn(Optional.of(detail(2,SystemRole.SYSTEM_ADMIN)));
        var result=service.changeRole(2L,SystemRole.SYSTEM_ADMIN,auth);
        assertEquals(SystemRole.SYSTEM_ADMIN,result.systemRole());
        assertEquals(SystemRole.SYSTEM_ADMIN,target.getSystemRole());
        verify(recorder).record(actor,AdminAuditActionType.USER_SYSTEM_ROLE_CHANGED,
                AdminAuditTargetType.USER,2L,"서비스 관리자를 지정했습니다.");

        assertThrows(BusinessException.class,()->service.changeRole(2L,SystemRole.SUPER_ADMIN,auth));
        when(users.findById(1L)).thenReturn(Optional.of(actor));
        BusinessException self=assertThrows(BusinessException.class,
                ()->service.changeRole(1L,SystemRole.USER,auth));
        assertEquals(ErrorCode.SELF_ADMIN_MODIFICATION_FORBIDDEN,self.getErrorCode());
    }

    @Test void superAdminTargetCannotBeChangedAndAdminStatusChangesAreLimitedToOrdinaryUsers() {
        User actor=user(1,SystemRole.SUPER_ADMIN);
        User target=user(2,SystemRole.SUPER_ADMIN);
        when(authorization.requireSuperAdmin(auth)).thenReturn(actor);
        when(authorization.requireAdmin(auth)).thenReturn(actor);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        BusinessException role=assertThrows(BusinessException.class,
                ()->service.changeRole(2L,SystemRole.USER,auth));
        assertEquals(ErrorCode.SUPER_ADMIN_MODIFICATION_FORBIDDEN,role.getErrorCode());
        BusinessException status=assertThrows(BusinessException.class,
                ()->service.changeStatus(2L,UserStatus.BLOCKED,auth));
        assertEquals(ErrorCode.SYSTEM_ADMIN_TARGET_FORBIDDEN,status.getErrorCode());
        verifyNoInteractions(recorder);
    }

    private User user(long uid,SystemRole role) {
        User user=User.create("user"+uid,null);
        org.springframework.test.util.ReflectionTestUtils.setField(user,"userUid",uid);
        user.changeSystemRole(role); return user;
    }
    private AdminUserDetailResponse detail(long uid,SystemRole role) {
        return new AdminUserDetailResponse(uid,"login"+uid,"user"+uid,UserStatus.ACTIVE,role,
                null,null,0,0);
    }
}
