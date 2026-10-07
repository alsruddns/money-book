package com.moneybook.backend.moneybook.provider;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.InvitationStatus;
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

class MoneyBookBackupPermissionTests {
    private final UserRepository users=mock(UserRepository.class);
    private final MoneyBookRepository books=mock(MoneyBookRepository.class);
    private final MoneyBookUserRepository members=mock(MoneyBookUserRepository.class);
    private final MoneyBookPermissionProvider permissions=new MoneyBookPermissionProvider(users,books,members);

    @Test void ownerAndAcceptedAdminMayBackupButReadOnlyAndPendingAdminMayNot() {
        User owner=User.create("owner",null); ReflectionTestUtils.setField(owner,"userUid",1L);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        MoneyBook book=MoneyBook.create("house",1L); ReflectionTestUtils.setField(book,"moneyBookUid",10L);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        var ownerAuth=auth(1L);
        assertSame(book,permissions.requireBackupAccess(10L,ownerAuth));

        User admin=User.create("admin",null); ReflectionTestUtils.setField(admin,"userUid",2L);
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        var adminAuth=auth(2L);
        MoneyBookUser acceptedAdmin=MoneyBookUser.invite(book,2L,true,false,false,false,false);
        when(members.findByMoneyBookUidAndUserUid(10L,2L)).thenReturn(Optional.of(acceptedAdmin));
        BusinessException pendingForbidden=assertThrows(BusinessException.class,()->permissions.requireBackupAccess(10L,adminAuth));
        assertEquals(ErrorCode.MONEY_BOOK_BACKUP_FORBIDDEN,pendingForbidden.getErrorCode());
        acceptedAdmin.acceptInvitation();
        when(members.findByMoneyBookUidAndUserUid(10L,2L)).thenReturn(Optional.of(acceptedAdmin));
        assertSame(book,permissions.requireBackupAccess(10L,adminAuth));

        User reader=User.create("reader",null); ReflectionTestUtils.setField(reader,"userUid",3L);
        when(users.findById(3L)).thenReturn(Optional.of(reader));
        var readerAuth=auth(3L);
        MoneyBookUser readOnly=MoneyBookUser.invite(book,3L,false,false,true,false,false); readOnly.acceptInvitation();
        when(members.findByMoneyBookUidAndUserUid(10L,3L)).thenReturn(Optional.of(readOnly));
        BusinessException forbidden=assertThrows(BusinessException.class,()->permissions.requireBackupAccess(10L,readerAuth));
        assertEquals(ErrorCode.MONEY_BOOK_BACKUP_FORBIDDEN,forbidden.getErrorCode());
    }

    private JwtAuthenticationToken auth(long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("token").header("alg","none").subject(Long.toString(uid))
                .issuedAt(Instant.now().minusSeconds(1)).expiresAt(Instant.now().plusSeconds(30)).build());
    }
}
