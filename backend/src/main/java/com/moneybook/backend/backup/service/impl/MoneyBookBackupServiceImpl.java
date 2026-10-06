package com.moneybook.backend.backup.service.impl;

import com.moneybook.backend.activity.ActivityRecorder;
import com.moneybook.backend.backup.dto.*;
import com.moneybook.backend.backup.repository.BackupRepository;
import com.moneybook.backend.backup.service.MoneyBookBackupService;
import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.*;
import com.moneybook.backend.moneybook.provider.MoneyBookPermissionProvider;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.recurring.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoneyBookBackupServiceImpl implements MoneyBookBackupService {
    private static final Logger log = LoggerFactory.getLogger(MoneyBookBackupServiceImpl.class);
    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final int BATCH_SIZE = 500;
    private static final int MAX_CATEGORIES = 1_000, MAX_ACCOUNTS = 500,
            MAX_TRANSACTIONS = 200_000, MAX_TRANSFERS = 100_000,
            MAX_RECURRING = 5_000, MAX_BUDGETS = 1_000, MAX_CLOSINGS = 1_000;

    private final ObjectMapper objectMapper;
    private final BackupRepository backupRepository;
    private final MoneyBookPermissionProvider permissions;
    private final MoneyBookRepository moneyBooks;
    private final MoneyBookUserRepository memberships;
    private final CategoryRepository categories;
    private final AccountRepository accounts;
    private final RecurringTransactionRepository recurring;
    private final BudgetRepository budgets;
    private final MonthClosingRepository closings;
    private final ActivityRecorder activityRecorder;

    /** 관리자 수준 권한으로 가계부 업무 데이터만 명시적 DTO에 기록한다. */
    @Override
    @Transactional(readOnly = true)
    public void writeBackup(Long bookUid, Authentication authentication, OutputStream output) {
        MoneyBook book = permissions.requireBackupAccess(bookUid, authentication);
        List<MoneyBookCategory> categoryRows = backupRepository.findRows(MoneyBookCategory.class, bookUid);
        List<MoneyBookAccount> accountRows = backupRepository.findRows(MoneyBookAccount.class, bookUid);
        List<RecurringTransaction> recurringRows = backupRepository.findRows(RecurringTransaction.class, bookUid);
        List<MoneyBookBudget> budgetRows = backupRepository.findRows(MoneyBookBudget.class, bookUid);
        List<MoneyBookMonthClosing> closingRows = backupRepository.findRows(MoneyBookMonthClosing.class, bookUid);
        WeekStartDay startDay = backupRepository.findRows(MoneyBookSetting.class, bookUid).stream()
                .findFirst().map(MoneyBookSetting::getWeekStartDay).orElse(WeekStartDay.SUNDAY);
        Set<Long> knownRules = recurringRows.stream().map(RecurringTransaction::getRecurringTransactionUid).collect(Collectors.toSet());
        try (JsonGenerator generator = objectMapper.tokenStreamFactory().createGenerator(output)) {
            Set<Long> orphanRuleIds = findOrphanRuleIds(bookUid, knownRules);
            generator.writeStartObject();
            generator.writeNumberProperty("backupVersion", MoneyBookBackupDocument.BACKUP_VERSION);
            generator.writeStringProperty("exportedAt", LocalDateTime.now().toString());
            generator.writeName("moneyBook");
            objectMapper.writeValue(generator, new MoneyBookBackupDocument.BookData(book.getName()));
            generator.writeName("setting");
            objectMapper.writeValue(generator, new MoneyBookBackupDocument.SettingData(startDay.name()));
            writeArray(generator, "categories", categoryRows.stream().map(c -> new MoneyBookBackupDocument.CategoryData(
                    c.getCategoryUid(), c.getName(), c.getTransactionType().name(), c.getSortOrder())).toList());
            writeArray(generator, "accounts", accountRows.stream().map(a -> new MoneyBookBackupDocument.AccountData(
                    a.getAccountUid(), a.getName(), a.getAccountType().name(), a.getSortOrder())).toList());
            writeTransactions(generator, bookUid);
            writeTransfers(generator, bookUid);
            writeArray(generator, "recurringTransactions", recurringRows.stream().map(r -> new MoneyBookBackupDocument.RecurringData(
                    r.getRecurringTransactionUid(), r.getTransactionType().name(), r.getAmount().toPlainString(),
                    r.getCategory().getCategoryUid(), r.getAccount().getAccountUid(), r.getFrequency().name(),
                    r.getDayOfMonth(), r.getDayOfWeek(), r.getStartDate().toString(), date(r.getEndDate()),
                    r.getMemo(), r.isActive(), date(r.getLastGeneratedDate()))).toList());
            writeArray(generator, "budgets", budgetRows.stream().map(b -> new MoneyBookBackupDocument.BudgetData(
                    b.getBudgetUid(), b.getYear(), b.getMonth(), decimal(b.getTotalBudget()),
                    b.getCategories().stream().map(c -> new MoneyBookBackupDocument.CategoryBudgetData(
                            c.getCategory().getCategoryUid(), c.getAmount().toPlainString())).toList())).toList());
            writeArray(generator, "monthClosings", closingRows.stream().map(c -> new MoneyBookBackupDocument.ClosingData(
                    c.getYear(), c.getMonth(), c.getIncome().toPlainString(), c.getExpense().toPlainString(),
                    c.getTransactionCount(), c.getPreviousIncome().toPlainString(), c.getPreviousExpense().toPlainString(),
                    c.isBudgetConfigured(), decimal(c.getTotalBudget()), c.getClosedAt().toString())).toList());
            writeArray(generator, "orphanRecurringSourceUids", orphanRuleIds.stream().sorted().toList());
            generator.writeEndObject();
            generator.flush();
            activityRecorder.record(bookUid, authentication, ActivityType.BACKUP_EXPORTED,
                    ActivityTargetType.BACKUP, null, "가계부 전체 데이터를 백업했습니다.", null);
        }
        catch (IOException exception) { throw new BusinessException(ErrorCode.BACKUP_EXPORT_FAILED); }
    }

    private Set<Long> findOrphanRuleIds(Long bookUid, Set<Long> knownRules) throws IOException {
        Set<Long> orphanIds = new HashSet<>();
        forEachTransactionBatch(bookUid, row -> {
            Long ruleUid = row.getRecurringTransactionUid();
            if (ruleUid != null && !knownRules.contains(ruleUid)) orphanIds.add(ruleUid);
        });
        return orphanIds;
    }

    private void writeTransactions(JsonGenerator generator, Long bookUid) throws IOException {
        generator.writeName("transactions");
        generator.writeStartArray();
        forEachTransactionBatch(bookUid, row -> objectMapper.writeValue(generator,
                new MoneyBookBackupDocument.TransactionData(row.getTransactionUid(), row.getTransactionType().name(),
                        row.getAmount().toPlainString(), row.getTransactionDate().toString(),
                        row.getCategory().getCategoryUid(), row.getAccount().getAccountUid(), row.getMemo(),
                        row.getRecurringTransactionUid(), date(row.getScheduledDate()))));
        generator.writeEndArray();
    }

    private void writeTransfers(JsonGenerator generator, Long bookUid) throws IOException {
        generator.writeName("transfers");
        generator.writeStartArray();
        long afterUid = 0;
        while (true) {
            List<MoneyBookTransfer> batch = backupRepository.transferBatch(bookUid, afterUid, BATCH_SIZE);
            if (batch.isEmpty()) break;
            for (MoneyBookTransfer row : batch) {
                objectMapper.writeValue(generator, new MoneyBookBackupDocument.TransferData(row.getTransferUid(),
                        row.getFromAccount().getAccountUid(), row.getToAccount().getAccountUid(),
                        row.getAmount().toPlainString(), row.getTransferDate().toString(), row.getMemo()));
            }
            afterUid = batch.getLast().getTransferUid();
            if (batch.size() < BATCH_SIZE) break;
        }
        generator.writeEndArray();
    }

    private void forEachTransactionBatch(Long bookUid, TransactionBatchConsumer consumer) throws IOException {
        long afterUid = 0;
        while (true) {
            List<MoneyBookTransaction> batch = backupRepository.transactionBatch(bookUid, afterUid, BATCH_SIZE);
            if (batch.isEmpty()) break;
            for (MoneyBookTransaction row : batch) consumer.accept(row);
            afterUid = batch.getLast().getTransactionUid();
            if (batch.size() < BATCH_SIZE) break;
        }
    }

    private void writeArray(JsonGenerator generator, String name, List<?> values) throws IOException {
        generator.writeName(name);
        generator.writeStartArray();
        for (Object value : values) objectMapper.writeValue(generator, value);
        generator.writeEndArray();
    }

    @FunctionalInterface
    private interface TransactionBatchConsumer {
        void accept(MoneyBookTransaction transaction) throws IOException;
    }

    @Override
    @Transactional(readOnly = true)
    public BackupValidationResponse validate(MultipartFile file, Authentication authentication) {
        permissions.currentActiveUserUid(authentication);
        return validation(readDocument(file));
    }

    /** 검증 결과가 유효할 때만 새 가계부를 단일 트랜잭션으로 구성한다. */
    @Override
    @Transactional
    public BackupRestoreResponse restore(MultipartFile file, Authentication authentication) {
        Long userUid = permissions.currentActiveUserUid(authentication);
        MoneyBookBackupDocument doc = readDocument(file);
        BackupValidationResponse result = validation(doc);
        if (!result.valid()) {
            ErrorCode errorCode = doc == null || doc.backupVersion() != MoneyBookBackupDocument.BACKUP_VERSION
                    ? ErrorCode.BACKUP_VERSION_UNSUPPORTED
                    : result.errors().stream().anyMatch(message -> message.contains("중복"))
                        ? ErrorCode.BACKUP_DUPLICATE_IDENTIFIER : ErrorCode.BACKUP_REFERENCE_INVALID;
            throw new BusinessException(errorCode);
        }
        try {
        MoneyBook book = moneyBooks.save(MoneyBook.create(doc.moneyBook().name(), userUid));
        memberships.save(MoneyBookUser.owner(book, userUid));
        backupRepository.save(MoneyBookSetting.create(book, WeekStartDay.valueOf(doc.setting().weekStartDay())));
        Map<Long, MoneyBookCategory> categoryMap = new HashMap<>();
        for (var row : doc.categories()) {
            MoneyBookCategory saved = categories.save(MoneyBookCategory.create(book, row.name(), TransactionType.valueOf(row.transactionType()), row.sortOrder()));
            categoryMap.put(row.categoryUid(), saved);
        }
        Map<Long, MoneyBookAccount> accountMap = new HashMap<>();
        for (var row : doc.accounts()) {
            MoneyBookAccount saved = accounts.save(MoneyBookAccount.create(book, row.name(), AccountType.valueOf(row.accountType()), row.sortOrder()));
            accountMap.put(row.accountUid(), saved);
        }
        Map<Long, Long> recurringMap = new HashMap<>();
        for (var row : doc.recurringTransactions()) {
            RecurringTransaction saved = recurring.save(RecurringTransaction.create(book,
                    TransactionType.valueOf(row.transactionType()), amount(row.amount()), categoryMap.get(row.categoryUid()),
                    accountMap.get(row.accountUid()), RecurringFrequency.valueOf(row.frequency()), row.dayOfMonth(),
                    row.dayOfWeek(), LocalDate.parse(row.startDate()), parseDate(row.endDate()), row.memo()));
            if (!row.active()) saved.changeActive(false);
            if (row.lastGeneratedDate() != null) saved.recordGeneratedThrough(LocalDate.parse(row.lastGeneratedDate()));
            recurringMap.put(row.recurringTransactionUid(), saved.getRecurringTransactionUid());
        }
        Map<Long, Long> orphanMap = new HashMap<>();
        long orphanUid = -1;
        for (Long oldUid : doc.orphanRecurringSourceUids()) orphanMap.put(oldUid, orphanUid--);
        int inserted = 0;
        for (var row : doc.transactions()) {
            Long newRecurringUid = row.recurringTransactionUid() == null ? null
                    : recurringMap.getOrDefault(row.recurringTransactionUid(), orphanMap.get(row.recurringTransactionUid()));
            MoneyBookTransaction entity = newRecurringUid == null
                    ? MoneyBookTransaction.create(book, TransactionType.valueOf(row.transactionType()), amount(row.amount()),
                        LocalDate.parse(row.transactionDate()), categoryMap.get(row.categoryUid()), accountMap.get(row.accountUid()), row.memo())
                    : MoneyBookTransaction.createRecurring(book, TransactionType.valueOf(row.transactionType()), amount(row.amount()),
                        LocalDate.parse(row.scheduledDate()), categoryMap.get(row.categoryUid()), accountMap.get(row.accountUid()), row.memo(), newRecurringUid);
            if (newRecurringUid != null && !LocalDate.parse(row.transactionDate()).equals(LocalDate.parse(row.scheduledDate()))) {
                entity.change(TransactionType.valueOf(row.transactionType()), amount(row.amount()), LocalDate.parse(row.transactionDate()), categoryMap.get(row.categoryUid()), accountMap.get(row.accountUid()), row.memo());
            }
            backupRepository.save(entity);
            if (++inserted % BATCH_SIZE == 0) { backupRepository.flush(); backupRepository.clear(); }
        }
        inserted = 0;
        for (var row : doc.transfers()) {
            backupRepository.save(MoneyBookTransfer.create(book, accountMap.get(row.fromAccountUid()), accountMap.get(row.toAccountUid()), amount(row.amount()), LocalDate.parse(row.transferDate()), row.memo()));
            if (++inserted % BATCH_SIZE == 0) { backupRepository.flush(); backupRepository.clear(); }
        }
        for (var row : doc.budgets()) {
            MoneyBookBudget budget = MoneyBookBudget.create(book, row.year(), row.month(), nullableAmount(row.totalBudget()));
            for (var item : row.categories()) budget.addCategory(categoryMap.get(item.categoryUid()), amount(item.amount()));
            budgets.save(budget);
        }
        for (var row : doc.monthClosings()) closings.save(MoneyBookMonthClosing.create(book, row.year(), row.month(), amount(row.income()), amount(row.expense()), row.transactionCount(), amount(row.previousIncome()), amount(row.previousExpense()), row.budgetConfigured(), nullableAmount(row.totalBudget()), userUid, LocalDateTime.parse(row.closedAt())));
        backupRepository.flush();
        int categoryBudgetCount = doc.budgets().stream().mapToInt(b -> b.categories().size()).sum();
        activityRecorder.record(book.getMoneyBookUid(), authentication, ActivityType.BACKUP_RESTORED,
                ActivityTargetType.BACKUP, null, "백업 데이터로 가계부를 복원했습니다.", null);
        return new BackupRestoreResponse(book.getMoneyBookUid(), book.getName(), doc.categories().size(), doc.accounts().size(), doc.transactions().size(), doc.transfers().size(), doc.recurringTransactions().size(), doc.budgets().size(), categoryBudgetCount, doc.monthClosings().size());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("MoneyBook backup restore failed userUid={} cause={}", userUid,
                    exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.BACKUP_RESTORE_FAILED);
        }
    }

    private MoneyBookBackupDocument readDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.BACKUP_FILE_INVALID);
        if (file.getSize() > MAX_FILE_BYTES) throw new BusinessException(ErrorCode.BACKUP_FILE_TOO_LARGE);
        try { return objectMapper.readValue(file.getBytes(), MoneyBookBackupDocument.class); }
        catch (Exception exception) { throw new BusinessException(ErrorCode.BACKUP_FILE_INVALID); }
    }

    private BackupValidationResponse validation(MoneyBookBackupDocument d) {
        try {
            return validateDocument(d);
        } catch (RuntimeException exception) {
            return invalid(new ArrayList<>());
        }
    }

    private BackupValidationResponse validateDocument(MoneyBookBackupDocument d) {
        List<String> errors = new ArrayList<>();
        if (d == null) return invalid(errors);
        if (d.backupVersion() != MoneyBookBackupDocument.BACKUP_VERSION) errors.add("지원하지 않는 백업 버전입니다.");
        if (d.exportedAt() == null || d.exportedAt().length() > 40) errors.add("백업 생성 시각이 올바르지 않습니다.");
        if (d.moneyBook() == null || blankOrLong(d.moneyBook().name(), 100)) errors.add("가계부 이름이 올바르지 않습니다.");
        if (d.setting() == null || !enumValue(WeekStartDay.class, d.setting().weekStartDay())) errors.add("가계부 설정이 올바르지 않습니다.");
        if (d.categories()==null || d.accounts()==null || d.transactions()==null || d.transfers()==null
                || d.recurringTransactions()==null || d.budgets()==null || d.monthClosings()==null
                || d.orphanRecurringSourceUids()==null) errors.add("백업 필수 목록이 누락되었습니다.");
        List<MoneyBookBackupDocument.CategoryData> cats = safe(d.categories());
        List<MoneyBookBackupDocument.AccountData> accts = safe(d.accounts());
        List<MoneyBookBackupDocument.TransactionData> txs = safe(d.transactions());
        List<MoneyBookBackupDocument.TransferData> xfers = safe(d.transfers());
        List<MoneyBookBackupDocument.RecurringData> rules = safe(d.recurringTransactions());
        List<MoneyBookBackupDocument.BudgetData> budgetRows = safe(d.budgets());
        List<MoneyBookBackupDocument.ClosingData> closingRows = safe(d.monthClosings());
        if (cats.size()>MAX_CATEGORIES || accts.size()>MAX_ACCOUNTS || txs.size()>MAX_TRANSACTIONS || xfers.size()>MAX_TRANSFERS || rules.size()>MAX_RECURRING || budgetRows.size()>MAX_BUDGETS || closingRows.size()>MAX_CLOSINGS) errors.add("백업 데이터 건수 제한을 초과했습니다.");
        Set<Long> catIds = ids(cats.stream().map(MoneyBookBackupDocument.CategoryData::categoryUid).toList(), errors);
        Set<Long> accountIds = ids(accts.stream().map(MoneyBookBackupDocument.AccountData::accountUid).toList(), errors);
        Set<Long> ruleIds = ids(rules.stream().map(MoneyBookBackupDocument.RecurringData::recurringTransactionUid).toList(), errors);
        ids(txs.stream().map(MoneyBookBackupDocument.TransactionData::transactionUid).toList(), errors);
        ids(xfers.stream().map(MoneyBookBackupDocument.TransferData::transferUid).toList(), errors);
        ids(budgetRows.stream().map(MoneyBookBackupDocument.BudgetData::budgetUid).toList(), errors);
        Set<Long> orphanIds = ids(safe(d.orphanRecurringSourceUids()), errors);
        if (ruleIds.stream().anyMatch(orphanIds::contains)) errors.add("정기 거래 식별자가 중복되었습니다.");
        Set<String> categoryNames = new HashSet<>();
        for (var c : cats) {
            if (blankOrLong(c.name(),100) || !enumValue(TransactionType.class,c.transactionType()) || c.sortOrder()<0) errors.add("카테고리 데이터가 올바르지 않습니다.");
            else if (!categoryNames.add(c.transactionType()+"/"+c.name())) errors.add("가계부 안에 동일한 유형과 이름의 카테고리가 중복되었습니다.");
        }
        Set<String> accountNames = new HashSet<>();
        for (var a : accts) {
            if (blankOrLong(a.name(),100) || !enumValue(AccountType.class,a.accountType()) || a.sortOrder()<0) errors.add("계좌 데이터가 올바르지 않습니다.");
            else if (!accountNames.add(a.name())) errors.add("가계부 안에 동일한 이름의 계좌가 중복되었습니다.");
        }
        for (var r : rules) {
            MoneyBookBackupDocument.CategoryData ruleCategory = cats.stream().filter(c -> Objects.equals(c.categoryUid(), r.categoryUid())).findFirst().orElse(null);
            if (!enumValue(TransactionType.class,r.transactionType()) || !enumValue(RecurringFrequency.class,r.frequency()) || !catIds.contains(r.categoryUid()) || ruleCategory == null || !Objects.equals(ruleCategory.transactionType(), r.transactionType()) || !accountIds.contains(r.accountUid()) || !validPositiveAmount(r.amount()) || !validDate(r.startDate()) || (r.endDate()!=null && (!validDate(r.endDate()) || LocalDate.parse(r.endDate()).isBefore(LocalDate.parse(r.startDate())))) || (r.lastGeneratedDate()!=null && !validDate(r.lastGeneratedDate())) || (r.memo()!=null && r.memo().length()>500)) errors.add("정기 거래 데이터가 올바르지 않습니다.");
            else { RecurringFrequency f=RecurringFrequency.valueOf(r.frequency()); if ((f==RecurringFrequency.MONTHLY && (r.dayOfMonth()==null||r.dayOfMonth()<1||r.dayOfMonth()>31||r.dayOfWeek()!=null)) || (f==RecurringFrequency.WEEKLY && (r.dayOfWeek()==null||r.dayOfWeek()<1||r.dayOfWeek()>7||r.dayOfMonth()!=null))) errors.add("정기 거래 일정이 올바르지 않습니다."); }
        }
        for (var t : txs) {
            MoneyBookBackupDocument.CategoryData c = cats.stream().filter(x->Objects.equals(x.categoryUid(),t.categoryUid())).findFirst().orElse(null);
            boolean generated=t.recurringTransactionUid()!=null;
            if (!enumValue(TransactionType.class,t.transactionType()) || c==null || !Objects.equals(c.transactionType(),t.transactionType()) || !accountIds.contains(t.accountUid()) || !validPositiveAmount(t.amount()) || !validDate(t.transactionDate()) || (t.memo()!=null&&t.memo().length()>500) || (generated && ((!ruleIds.contains(t.recurringTransactionUid())&&!orphanIds.contains(t.recurringTransactionUid())) || !validDate(t.scheduledDate())))) errors.add("거래 참조 또는 값이 올바르지 않습니다.");
        }
        Set<String> occurrences = new HashSet<>();
        for (var t : txs) if (t.recurringTransactionUid()!=null && t.scheduledDate()!=null && !occurrences.add(t.recurringTransactionUid()+"/"+t.scheduledDate())) errors.add("정기 거래 발생일이 중복되었습니다.");
        for (var t : xfers) if (!accountIds.contains(t.fromAccountUid()) || !accountIds.contains(t.toAccountUid()) || Objects.equals(t.fromAccountUid(),t.toAccountUid()) || !validPositiveAmount(t.amount()) || !validDate(t.transferDate()) || (t.memo()!=null&&t.memo().length()>500)) errors.add("이체 데이터가 올바르지 않습니다.");
        Set<String> budgetMonths=new HashSet<>();
        int categoryBudgetCount = 0;
        for (var b : budgetRows) {
            if (b.month()<1||b.month()>12||b.year()<1||b.year()>9999||!budgetMonths.add(b.year()+"/"+b.month())||!validAmount(b.totalBudget(),true)||b.categories()==null) errors.add("예산 데이터가 올바르지 않습니다.");
            Set<Long> used=new HashSet<>(); BigDecimal categorySum=BigDecimal.ZERO;
            for(var c:safe(b.categories())) {
                categoryBudgetCount++;
                MoneyBookBackupDocument.CategoryData budgetCategory=c==null?null:cats.stream().filter(v->Objects.equals(v.categoryUid(),c.categoryUid())).findFirst().orElse(null);
                if(c==null||!catIds.contains(c.categoryUid())||!used.add(c.categoryUid())||!validAmount(c.amount(),false)||budgetCategory==null||!TransactionType.EXPENSE.name().equals(budgetCategory.transactionType())) errors.add("카테고리 예산 참조가 올바르지 않습니다.");
                if(c!=null&&validAmount(c.amount(),false)) categorySum=categorySum.add(new BigDecimal(c.amount()));
            }
            if(b.totalBudget()!=null&&validAmount(b.totalBudget(),false)&&categorySum.compareTo(new BigDecimal(b.totalBudget()))>0) errors.add("카테고리 예산 합계가 총 예산을 초과합니다.");
        }
        if(categoryBudgetCount>100_000) errors.add("카테고리 예산 건수 제한을 초과했습니다.");
        Set<String> closeMonths=new HashSet<>();
        for (var c : closingRows) if(c.month()<1||c.month()>12||c.year()<1||c.year()>9999||c.transactionCount()<0||!closeMonths.add(c.year()+"/"+c.month())||!validAmount(c.income(),false)||!validAmount(c.expense(),false)||!validAmount(c.previousIncome(),false)||!validAmount(c.previousExpense(),false)||!validAmount(c.totalBudget(),true)||!validDateTime(c.closedAt())) errors.add("월 마감 snapshot이 올바르지 않습니다.");
        List<LocalDate> dates=txs.stream().map(t->parseDate(t.transactionDate())).filter(Objects::nonNull).sorted().toList();
        return new BackupValidationResponse(errors.isEmpty(),d.backupVersion(),d.moneyBook()==null?null:d.moneyBook().name(),cats.size(),accts.size(),txs.size(),xfers.size(),rules.size(),budgetRows.size(),categoryBudgetCount,closingRows.size(),dates.isEmpty()?null:dates.getFirst(),dates.isEmpty()?null:dates.getLast(),List.of(),List.copyOf(errors));
    }
    private BackupValidationResponse invalid(List<String> errors) { errors.add("백업 JSON 구조가 올바르지 않습니다."); return new BackupValidationResponse(false,0,null,0,0,0,0,0,0,0,0,null,null,List.of(),List.copyOf(errors)); }
    private Set<Long> ids(List<Long> values,List<String> errors) { Set<Long> result=new HashSet<>(); for(Long id:values) if(id==null||id<=0||!result.add(id)) errors.add("백업 식별자가 없거나 중복되었습니다."); return result; }
    private <T> List<T> safe(List<T> values) { return values==null?List.of():values; }
    private boolean enumValue(Class<? extends Enum<?>> type,String value) {
        if (value == null) return false;
        for (Enum<?> item : type.getEnumConstants()) if (item.name().equals(value)) return true;
        return false;
    }
    private boolean blankOrLong(String value,int max) { return value==null||value.isBlank()||value.length()>max; }
    private boolean validAmount(String value,boolean nullable) { if(value==null)return nullable; try { BigDecimal b=new BigDecimal(value); return b.signum()>=0&&b.scale()<=2&&b.precision()-b.scale()<=17; } catch(Exception e){return false;} }
    private boolean validPositiveAmount(String value) { if (!validAmount(value, false)) return false; return new BigDecimal(value).signum() > 0; }
    private BigDecimal amount(String value) { return new BigDecimal(value); }
    private BigDecimal nullableAmount(String value) { return value==null?null:new BigDecimal(value); }
    private boolean validDate(String value) { return parseDate(value)!=null; }
    private LocalDate parseDate(String value) { try{return value==null?null:LocalDate.parse(value);}catch(Exception e){return null;} }
    private boolean validDateTime(String value) { try{LocalDateTime.parse(value);return true;}catch(Exception e){return false;} }
    private String date(LocalDate value) { return value==null?null:value.toString(); }
    private String decimal(BigDecimal value) { return value==null?null:value.toPlainString(); }
}
