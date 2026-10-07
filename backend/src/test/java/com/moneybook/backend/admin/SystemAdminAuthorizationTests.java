package com.moneybook.backend.admin;

import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemAdminAuthorizationTests {
    private final UserRepository users=mock(UserRepository.class);
    private final MoneyBookRepository books=mock(MoneyBookRepository.class);
    private final MoneyBookUserRepository memberships=mock(MoneyBookUserRepository.class);
    private final SystemAdminAuthorizationProvider admin=new SystemAdminAuthorizationProvider(users);

    @Test void adminAccessUsesCurrentDatabaseRoleAndStatus() {
        User user=user(8L,SystemRole.SYSTEM_ADMIN,UserStatus.ACTIVE);
        when(users.findById(8L)).thenReturn(Optional.of(user));
        var auth=auth(8);
        assertTrue(admin.hasAdminAccess(auth));
        user.changeStatus(UserStatus.BLOCKED);
        assertFalse(admin.hasAdminAccess(auth));
        user.changeStatus(UserStatus.ACTIVE);
        user.changeSystemRole(SystemRole.USER);
        assertFalse(admin.hasAdminAccess(auth));
    }

    @Test void superAdminDoesNotBypassMoneyBookMembershipAuthorization() {
        User superAdmin=user(8L,SystemRole.SUPER_ADMIN,UserStatus.ACTIVE);
        when(users.findById(8L)).thenReturn(Optional.of(superAdmin));
        MoneyBook book=MoneyBook.create("private",99L); ReflectionTestUtils.setField(book,"moneyBookUid",4L);
        when(books.findById(4L)).thenReturn(Optional.of(book));
        var permissions=new MoneyBookPermissionProvider(users,books,memberships);
        BusinessException exception=assertThrows(BusinessException.class,
                ()->permissions.require(4L,auth(8),com.moneybook.backend.enums.MoneyBookPermission.READ));
        assertEquals(ErrorCode.MONEY_BOOK_READ_FORBIDDEN,exception.getErrorCode());
        verify(memberships).findByMoneyBookUidAndUserUid(4L,8L);
    }

    private User user(long uid,SystemRole role,UserStatus status) {
        User user=User.create("admin",null); ReflectionTestUtils.setField(user,"userUid",uid);
        user.changeSystemRole(role); user.changeStatus(status); return user;
    }
    private JwtAuthenticationToken auth(long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","none")
                .subject(Long.toString(uid)).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());
    }
}
