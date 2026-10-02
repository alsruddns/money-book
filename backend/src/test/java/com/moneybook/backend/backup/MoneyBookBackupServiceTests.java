package com.moneybook.backend.backup;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.backup.dto.MoneyBookBackupDocument;
import com.moneybook.backend.backup.repository.BackupRepository;
import com.moneybook.backend.backup.service.impl.MoneyBookBackupServiceImpl;
import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.*;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.recurring.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MoneyBookBackupServiceTests {
    private final BackupRepository backup = mock(BackupRepository.class);
    private final MoneyBookPermissionProvider permissions = mock(MoneyBookPermissionProvider.class);
    private final MoneyBookRepository books = mock(MoneyBookRepository.class);
    private final MoneyBookUserRepository memberships = mock(MoneyBookUserRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final RecurringTransactionRepository recurring = mock(RecurringTransactionRepository.class);
    private final BudgetRepository budgets = mock(BudgetRepository.class);
    private final MonthClosingRepository closings = mock(MonthClosingRepository.class);
    private final MoneyBookBackupServiceImpl service = new MoneyBookBackupServiceImpl(JsonMapper.builder().build(), backup,
            permissions, books, memberships, categories, accounts, recurring, budgets, closings);
    private final Authentication authentication = mock(Authentication.class);

    @BeforeEach void setUp() { when(permissions.currentActiveUserUid(authentication)).thenReturn(42L); }

    @Test void validatesVersionAndReturnsPreviewWithoutWriting() throws Exception {
        var result = service.validate(file(document()), authentication);
        assertTrue(result.valid());
        assertEquals(1, result.backupVersion());
        assertEquals("우리집", result.moneyBookName());
        assertEquals(1, result.transactionCount());
        assertEquals("2026-01-02", result.firstTransactionDate().toString());
        verify(backup, never()).save(any());
        verify(books, never()).save(any());
    }

    @Test void rejectsUnsupportedVersionAndDanglingReferences() throws Exception {
        var unsupported = document(999, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        assertFalse(service.validate(file(unsupported), authentication).valid());
        var dangling = document(1, List.of(), List.of(), List.of(tx(100, 999L, 200L, null)), List.of(), List.of(), List.of(), List.of());
        assertFalse(service.validate(file(dangling), authentication).valid());
    }

    @Test void rejectsDuplicateIdentifiersAndOccurrenceDates() throws Exception {
        var duplicate = new MoneyBookBackupDocument(1, "2026-10-02T10:00:00", new MoneyBookBackupDocument.BookData("우리집"),
                new MoneyBookBackupDocument.SettingData("SUNDAY"), List.of(cat(10), cat(10)), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        assertFalse(service.validate(file(duplicate), authentication).valid());
        var rule = new MoneyBookBackupDocument.RecurringData(30L,"EXPENSE","100",10L,20L,"MONTHLY",1,null,"2026-01-01",null,null,true,null);
        var repeated = document(1,List.of(cat(10)),List.of(account(20)),List.of(tx(101,10L,20L,30L),tx(102,10L,20L,30L)),List.of(rule),List.of(),List.of(),List.of());
        assertFalse(service.validate(file(repeated), authentication).valid());
    }

    @Test void rejectsTypeMismatchSameTransferAccountInvalidScheduleAndBudgetReference() throws Exception {
        var incomeCategory= new MoneyBookBackupDocument.CategoryData(11L,"급여","INCOME",0);
        var mismatch=document(1,List.of(incomeCategory),List.of(account(20)),List.of(tx(101,11L,20L,null)),List.of(),List.of(),List.of(),List.of());
        assertFalse(service.validate(file(mismatch),authentication).valid());
        var sameAccount=document(1,List.of(),List.of(account(20)),List.of(),List.of(),
                List.of(new MoneyBookBackupDocument.TransferData(1L,20L,20L,"10","2026-01-01",null)),List.of(),List.of());
        assertFalse(service.validate(file(sameAccount),authentication).valid());
        var badRule=new MoneyBookBackupDocument.RecurringData(30L,"EXPENSE","100",10L,20L,"MONTHLY",32,null,"2026-01-01",null,null,true,null);
        var badSchedule=document(1,List.of(cat(10)),List.of(account(20)),List.of(),List.of(badRule),List.of(),List.of(),List.of());
        assertFalse(service.validate(file(badSchedule),authentication).valid());
        var badBudget=document(1,List.of(cat(10)),List.of(),List.of(),List.of(),List.of(),
                List.of(new MoneyBookBackupDocument.BudgetData(40L,2026,13,"100",List.of())),List.of());
        assertFalse(service.validate(file(badBudget),authentication).valid());
    }

    @Test void rejectsMalformedAndOversizedFilesWithoutDatabaseWrites() throws Exception {
        var malformed = new MockMultipartFile("file", "bad.json", "application/json", "{".getBytes());
        BusinessException parseError = assertThrows(BusinessException.class, () -> service.validate(malformed, authentication));
        assertEquals(com.moneybook.backend.common.exception.ErrorCode.BACKUP_FILE_INVALID, parseError.getErrorCode());
        MultipartFile oversized = mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(MoneyBookBackupServiceImpl.MAX_FILE_BYTES + 1);
        BusinessException sizeError = assertThrows(BusinessException.class, () -> service.validate(oversized, authentication));
        assertEquals(com.moneybook.backend.common.exception.ErrorCode.BACKUP_FILE_TOO_LARGE, sizeError.getErrorCode());
        verify(backup, never()).save(any());
    }

    @Test void exportsOnlyVersionedBusinessDtosAndRequiresBackupAuthorization() throws Exception {
        MoneyBook book = MoneyBook.create("우리집", 42L);
        ReflectionTestUtils.setField(book, "moneyBookUid", 77L);
        when(permissions.requireBackupAccess(77L, authentication)).thenReturn(book);
        when(backup.findRows(MoneyBookCategory.class,77L)).thenReturn(List.of());
        when(backup.findRows(MoneyBookAccount.class,77L)).thenReturn(List.of());
        when(backup.findRows(MoneyBookTransaction.class,77L)).thenReturn(List.of());
        when(backup.findRows(MoneyBookTransfer.class,77L)).thenReturn(List.of());
        when(backup.findRows(RecurringTransaction.class,77L)).thenReturn(List.of());
        when(backup.findRows(MoneyBookBudget.class,77L)).thenReturn(List.of());
        when(backup.findRows(MoneyBookMonthClosing.class,77L)).thenReturn(List.of());
        when(backup.findRows(MoneyBookSetting.class,77L)).thenReturn(List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.writeBackup(77L,authentication,output);
        String json=output.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("\"backupVersion\":1"));
        assertTrue(json.contains("\"setting\":{"));
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("userAuth"));
        verify(permissions).requireBackupAccess(77L,authentication);
    }

    @Test void restoresAsNewOwnerAndRemapsCategoryAccountAndRecurringUids() throws Exception {
        when(books.save(any())).thenAnswer(inv -> { MoneyBook b=inv.getArgument(0); ReflectionTestUtils.setField(b,"moneyBookUid",700L); return b; });
        when(categories.save(any())).thenAnswer(inv -> { MoneyBookCategory c=inv.getArgument(0); ReflectionTestUtils.setField(c,"categoryUid",801L); return c; });
        java.util.concurrent.atomic.AtomicLong newAccountUid = new java.util.concurrent.atomic.AtomicLong(900L);
        when(accounts.save(any())).thenAnswer(inv -> { MoneyBookAccount a=inv.getArgument(0); ReflectionTestUtils.setField(a,"accountUid",newAccountUid.incrementAndGet()); return a; });
        when(recurring.save(any())).thenAnswer(inv -> { RecurringTransaction r=inv.getArgument(0); ReflectionTestUtils.setField(r,"recurringTransactionUid",1001L); return r; });
        var doc = new MoneyBookBackupDocument(1,"2026-10-02T10:00:00",new MoneyBookBackupDocument.BookData("우리집"),
                new MoneyBookBackupDocument.SettingData("MONDAY"),List.of(cat(10)),List.of(account(20),new MoneyBookBackupDocument.AccountData(21L,"은행","BANK",1)),
                List.of(tx(100,10L,20L,30L)),List.of(new MoneyBookBackupDocument.TransferData(50L,20L,21L,"25.00","2026-01-03","저축")),
                List.of(new MoneyBookBackupDocument.RecurringData(30L,"EXPENSE","100",10L,20L,"MONTHLY",1,null,"2026-01-01",null,null,true,null)),
                List.of(new MoneyBookBackupDocument.BudgetData(60L,2026,1,"500.00",List.of(new MoneyBookBackupDocument.CategoryBudgetData(10L,"300.00")))),
                List.of(new MoneyBookBackupDocument.ClosingData(2025,12,"1000.00","500.00",4,"900.00","450.00",true,"800.00","2026-01-01T12:00:00")),List.of());
        assertTrue(service.validate(file(doc), authentication).valid(), service.validate(file(doc), authentication).errors().toString());
        var response = service.restore(file(doc), authentication);
        assertEquals(700L,response.moneyBookUid());
        assertEquals(1,response.transactionCount());
        var membership = org.mockito.ArgumentCaptor.forClass(MoneyBookUser.class);
        verify(memberships).save(membership.capture());
        assertTrue(membership.getValue().isAdmin());
        assertTrue(membership.getValue().isCanCreate() && membership.getValue().isCanRead()
                && membership.getValue().isCanUpdate() && membership.getValue().isCanDelete());
        var transaction = org.mockito.ArgumentCaptor.forClass(MoneyBookTransaction.class);
        verify(backup).save(transaction.capture());
        assertEquals(801L,transaction.getValue().getCategory().getCategoryUid());
        assertEquals(901L,transaction.getValue().getAccount().getAccountUid());
        assertEquals(1001L,transaction.getValue().getRecurringTransactionUid());
        var transfer = org.mockito.ArgumentCaptor.forClass(MoneyBookTransfer.class);
        verify(backup).save(transfer.capture());
        assertEquals(901L,transfer.getValue().getFromAccount().getAccountUid());
        assertEquals(902L,transfer.getValue().getToAccount().getAccountUid());
        assertNotEquals(transfer.getValue().getFromAccount().getAccountUid(),transfer.getValue().getToAccount().getAccountUid());
        var budget = org.mockito.ArgumentCaptor.forClass(MoneyBookBudget.class);
        verify(budgets).save(budget.capture());
        assertEquals(801L,budget.getValue().getCategories().getFirst().getCategory().getCategoryUid());
        var closing = org.mockito.ArgumentCaptor.forClass(MoneyBookMonthClosing.class);
        verify(closings).save(closing.capture());
        assertEquals(42L,closing.getValue().getClosedByUserUid());
        assertEquals(new java.math.BigDecimal("1000.00"),closing.getValue().getIncome());
        var setting = org.mockito.ArgumentCaptor.forClass(MoneyBookSetting.class);
        verify(backup).save(setting.capture());
        assertEquals(WeekStartDay.MONDAY,setting.getValue().getWeekStartDay());
    }

    private MockMultipartFile file(MoneyBookBackupDocument document) throws Exception {
        return new MockMultipartFile("file", "backup.json", "application/json", JsonMapper.builder().build().writeValueAsBytes(document));
    }
    private MoneyBookBackupDocument document() { return document(1,List.of(cat(10)),List.of(account(20)),List.of(tx(100,10L,20L,null)),List.of(),List.of(),List.of(),List.of()); }
    private MoneyBookBackupDocument document(int version,List<MoneyBookBackupDocument.CategoryData> cats,List<MoneyBookBackupDocument.AccountData> accts,List<MoneyBookBackupDocument.TransactionData> txs,List<MoneyBookBackupDocument.RecurringData> rules,List<MoneyBookBackupDocument.TransferData> xfers,List<MoneyBookBackupDocument.BudgetData> bs,List<MoneyBookBackupDocument.ClosingData> cs) {
        return new MoneyBookBackupDocument(version,"2026-10-02T10:00:00",new MoneyBookBackupDocument.BookData("우리집"),new MoneyBookBackupDocument.SettingData("SUNDAY"),cats,accts,txs,xfers,rules,bs,cs,List.of());
    }
    private MoneyBookBackupDocument.CategoryData cat(long id) { return new MoneyBookBackupDocument.CategoryData(id,"식비","EXPENSE",0); }
    private MoneyBookBackupDocument.AccountData account(long id) { return new MoneyBookBackupDocument.AccountData(id,"현금","CASH",0); }
    private MoneyBookBackupDocument.TransactionData tx(long id,Long category,Long account,Long rule) { return new MoneyBookBackupDocument.TransactionData(id,"EXPENSE","100.00","2026-01-02",category,account,"점심",rule,rule==null?null:"2026-01-02"); }
}
