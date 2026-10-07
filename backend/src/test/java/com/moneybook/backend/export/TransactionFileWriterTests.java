package com.moneybook.backend.export;

import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchSort;
import com.moneybook.backend.transaction.repository.TransactionSearchRepository;
import com.moneybook.backend.export.service.impl.TransactionFileWriter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionFileWriterTests {
    private final TransactionSearchRepository repository = mock(TransactionSearchRepository.class);
    private final TransactionFileWriter writer = new TransactionFileWriter(repository);
    private final TransactionSearchRequest filters = new TransactionSearchRequest(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), null, null, null, null, null, null, 0, 20, TransactionSearchSort.DATE_DESC);

    @Test void csvUsesBomEscapingAndFormulaProtectionAndContinuesByKeyset() throws Exception {
        MoneyBookTransaction row = transaction("=1+1, says \"hi\"\nline");
        when(repository.findNextBatch(eq(5L),eq(filters),isNull(),isNull(),eq(1000))).thenReturn(java.util.Collections.nCopies(1000,row));
        when(repository.findNextBatch(eq(5L),eq(filters),eq(LocalDate.parse("2026-03-04")),eq(44L),eq(1000))).thenReturn(List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(1000, writer.csv(5L,filters,output));
        byte[] bytes=output.toByteArray();
        assertArrayEquals(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF},java.util.Arrays.copyOf(bytes,3));
        String csv=new String(bytes,3,bytes.length-3,java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"'=1+1, says \"\"hi\"\"\nline\""));
        assertTrue(csv.startsWith("\"거래일\""));
        verify(repository).findNextBatch(eq(5L),eq(filters),eq(LocalDate.parse("2026-03-04")),eq(44L),eq(1000));
    }

    @Test void xlsxIsReadableAndStoresAmountAsNumeric() throws Exception {
        MoneyBookTransaction row=transaction("한글 메모");
        when(repository.findNextBatch(eq(5L),eq(filters),isNull(),isNull(),eq(1000))).thenReturn(List.of(row));
        when(repository.findNextBatch(eq(5L),eq(filters),eq(LocalDate.parse("2026-03-04")),eq(44L),eq(1000))).thenReturn(List.of());
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        assertEquals(1,writer.xlsx(5L,filters,output));
        try(var workbook=WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet=workbook.getSheet("거래내역");
            assertEquals("거래일",sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC,sheet.getRow(1).getCell(2).getCellType());
            assertEquals(123.45,sheet.getRow(1).getCell(2).getNumericCellValue(),0.001);
            assertEquals("한글 메모",sheet.getRow(1).getCell(5).getStringCellValue());
        }
    }

    private MoneyBookTransaction transaction(String memo) {
        MoneyBook book=MoneyBook.create("가계부",1L); ReflectionTestUtils.setField(book,"moneyBookUid",5L);
        MoneyBookCategory category=MoneyBookCategory.create(book,"식비",TransactionType.EXPENSE,0); ReflectionTestUtils.setField(category,"categoryUid",11L);
        MoneyBookAccount account=MoneyBookAccount.create(book,"현금",com.moneybook.backend.enums.AccountType.CASH,0); ReflectionTestUtils.setField(account,"accountUid",12L);
        MoneyBookTransaction row=MoneyBookTransaction.create(book,TransactionType.EXPENSE,new BigDecimal("123.45"),LocalDate.parse("2026-03-04"),category,account,memo);
        ReflectionTestUtils.setField(row,"transactionUid",44L);
        return row;
    }
}
