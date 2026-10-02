package com.moneybook.backend.backup;

import com.moneybook.backend.backup.dto.MoneyBookBackupDocument;
import com.moneybook.backend.backup.repository.BackupRepository;
import com.moneybook.backend.backup.service.MoneyBookBackupService;
import com.moneybook.backend.closing.MonthClosingGuard;
import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.WeekStartDay;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookSettingRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.h2.api.Trigger;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:backup_restore;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class MoneyBookBackupIntegrationTests {
    @Autowired private MoneyBookBackupService service;
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private MoneyBookSettingRepository settings;
    @Autowired private BackupRepository backup;
    @Autowired private MonthClosingRepository closings;
    @Autowired private MonthClosingGuard closingGuard;
    @Autowired private JdbcTemplate jdbc;

    @Test void restoreCreatesNewOwnerAndRemapsAllBusinessReferencesWhileKeepingSnapshot() throws Exception {
        User owner=users.save(User.create("복원 사용자",null));
        var auth=new JwtAuthenticationToken(Jwt.withTokenValue("test-token").header("alg","none")
                .subject(owner.getUserUid().toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());
        var doc=new MoneyBookBackupDocument(1,"2026-10-02T10:00:00",new MoneyBookBackupDocument.BookData("백업 가계부"),
                new MoneyBookBackupDocument.SettingData("MONDAY"),
                List.of(new MoneyBookBackupDocument.CategoryData(700L,"식비","EXPENSE",0)),
                List.of(new MoneyBookBackupDocument.AccountData(800L,"현금","CASH",0),new MoneyBookBackupDocument.AccountData(801L,"은행","BANK",1)),
                List.of(new MoneyBookBackupDocument.TransactionData(900L,"EXPENSE","100.00","2025-12-02",700L,800L,"식사",950L,"2025-12-02")),
                List.of(new MoneyBookBackupDocument.TransferData(910L,800L,801L,"50.00","2025-12-03","저축")),
                List.of(new MoneyBookBackupDocument.RecurringData(950L,"EXPENSE","100.00",700L,800L,"MONTHLY",2,null,"2025-01-02",null,"식사",true,"2025-12-02")),
                List.of(new MoneyBookBackupDocument.BudgetData(920L,2025,12,"500.00",List.of(new MoneyBookBackupDocument.CategoryBudgetData(700L,"300.00")))),
                List.of(new MoneyBookBackupDocument.ClosingData(2025,11,"1000.00","500.00",4,"900.00","450.00",true,"800.00","2025-12-01T12:00:00")),List.of());
        MockMultipartFile file=new MockMultipartFile("file","backup.json","application/json",JsonMapper.builder().build().writeValueAsBytes(doc));
        var result=service.restore(file,auth);

        MoneyBook restored=books.findById(result.moneyBookUid()).orElseThrow();
        assertNotEquals(700L,backup.findRows(MoneyBookCategory.class,result.moneyBookUid()).getFirst().getCategoryUid());
        assertEquals(owner.getUserUid(),restored.getOwnerUserUid());
        assertEquals(owner.getUserUid(),memberships.findByMoneyBookUidAndUserUid(restored.getMoneyBookUid(),owner.getUserUid()).orElseThrow().getUserUid());
        assertEquals(WeekStartDay.MONDAY,settings.findByMoneyBookUid(restored.getMoneyBookUid()).orElseThrow().getWeekStartDay());
        MoneyBookTransaction transaction=backup.findRows(MoneyBookTransaction.class,restored.getMoneyBookUid()).getFirst();
        assertNotEquals(700L,transaction.getCategory().getCategoryUid());
        assertNotEquals(800L,transaction.getAccount().getAccountUid());
        assertNotEquals(950L,transaction.getRecurringTransactionUid());
        MoneyBookTransfer transfer=backup.findRows(MoneyBookTransfer.class,restored.getMoneyBookUid()).getFirst();
        assertNotEquals(800L,transfer.getFromAccount().getAccountUid());
        MoneyBookBudget budget=backup.findRows(MoneyBookBudget.class,restored.getMoneyBookUid()).getFirst();
        assertEquals(transaction.getCategory().getCategoryUid(),budget.getCategories().getFirst().getCategory().getCategoryUid());
        MoneyBookMonthClosing snapshot=closings.find(restored.getMoneyBookUid(),2025,11).orElseThrow();
        assertEquals(owner.getUserUid(),snapshot.getClosedByUserUid());
        assertEquals(new BigDecimal("1000.00"),snapshot.getIncome());
        ByteArrayOutputStream exported=new ByteArrayOutputStream();
        service.writeBackup(restored.getMoneyBookUid(),auth,exported);
        MockMultipartFile exportedFile=new MockMultipartFile("file","backup.json","application/json",exported.toByteArray());
        var preview=service.validate(exportedFile,auth);
        assertTrue(preview.valid(),preview.errors().toString());
        assertEquals(1,preview.transactionCount());
        assertEquals(1,preview.transferCount());
        assertEquals(1,preview.recurringTransactionCount());
        BusinessException closed=assertThrows(BusinessException.class,()->closingGuard.requireOpen(restored.getMoneyBookUid(), LocalDate.parse("2025-11-10")));
        assertEquals(ErrorCode.MONTH_CLOSED,closed.getErrorCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failureAfterInitialWritesRollsBackTheNewBookAndMembership() throws Exception {
        User owner=users.save(User.create("rollback user",null));
        var auth=new JwtAuthenticationToken(Jwt.withTokenValue("test-token").header("alg","none")
                .subject(owner.getUserUid().toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());
        jdbc.execute("CREATE TRIGGER fail_backup_tx BEFORE INSERT ON money_book_transactions FOR EACH ROW CALL 'com.moneybook.backend.backup.MoneyBookBackupIntegrationTests$FailTransactionTrigger'");
        try {
            var doc=new MoneyBookBackupDocument(1,"2026-10-02T10:00:00",new MoneyBookBackupDocument.BookData("rollback candidate"),
                    new MoneyBookBackupDocument.SettingData("SUNDAY"),List.of(new MoneyBookBackupDocument.CategoryData(700L,"식비","EXPENSE",0)),
                    List.of(new MoneyBookBackupDocument.AccountData(800L,"현금","CASH",0)),
                    List.of(new MoneyBookBackupDocument.TransactionData(900L,"EXPENSE","1.00","2026-01-01",700L,800L,"fail",null,null)),
                    List.of(),List.of(),List.of(),List.of(),List.of());
            MockMultipartFile file=new MockMultipartFile("file","backup.json","application/json",JsonMapper.builder().build().writeValueAsBytes(doc));
            BusinessException failure=assertThrows(BusinessException.class,()->service.restore(file,auth));
            assertEquals(ErrorCode.BACKUP_RESTORE_FAILED,failure.getErrorCode());
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM money_books WHERE name='rollback candidate'",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM money_book_users WHERE user_uid=?",Integer.class,owner.getUserUid()));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS fail_backup_tx");
        }
    }

    public static class FailTransactionTrigger implements Trigger {
        @Override public void fire(Connection connection,Object[] oldRow,Object[] newRow) throws SQLException {
            throw new SQLException("test-trigger");
        }
    }
}
