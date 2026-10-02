package com.moneybook.backend.export.service.impl;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.repository.TransactionSearchRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Writes CSV and streaming XLSX rows from fixed-size keyset batches. */
@Component
@RequiredArgsConstructor
public class TransactionFileWriter {
    public static final int BATCH_SIZE = 1000;
    private static final int XLSX_DATA_ROWS_PER_SHEET = 1_048_575;
    private static final String[] HEADERS = {"거래일", "구분", "금액", "카테고리", "계좌/결제수단", "메모",
            "정기거래", "등록일", "transactionUid"};

    private final TransactionSearchRepository transactions;

    public long csv(Long bookUid, TransactionSearchRequest filters, OutputStream output) {
        try {
            output.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            writeRow(writer, HEADERS);
            long count = eachBatch(bookUid, filters, rows -> {
                for (MoneyBookTransaction entry : rows) {
                    writeRow(writer, new String[]{entry.getTransactionDate().toString(), entry.getTransactionType().name(),
                            entry.getAmount().toPlainString(), entry.getCategory().getName(), entry.getAccount().getName(),
                            entry.getMemo() == null ? "" : entry.getMemo(), entry.getRecurringTransactionUid() == null ? "아니오" : "예",
                            entry.getRegTime() == null ? "" : entry.getRegTime().toString(),
                            entry.getTransactionUid().toString()});
                }
            });
            writer.flush();
            return count;
        } catch (IOException exception) {
            throw new IllegalStateException("CSV export stream failed", exception);
        }
    }

    public long xlsx(Long bookUid, TransactionSearchRequest filters, OutputStream output) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try (workbook) {
            Font font = workbook.createFont();
            font.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            CellStyle registeredStyle = workbook.createCellStyle();
            registeredStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            SheetState state = new SheetState(workbook, headerStyle, dateStyle, registeredStyle);
            long count = eachBatch(bookUid, filters, rows -> {
                for (MoneyBookTransaction entry : rows) {
                    if (state.sheetRow > XLSX_DATA_ROWS_PER_SHEET) state.nextSheet();
                    Row row = state.sheet.createRow(state.sheetRow++);
                    var dateCell = row.createCell(0);
                    dateCell.setCellValue(java.sql.Date.valueOf(entry.getTransactionDate()));
                    dateCell.setCellStyle(state.dateStyle);
                    row.createCell(1).setCellValue(entry.getTransactionType().name());
                    row.createCell(2).setCellValue(entry.getAmount().doubleValue());
                    row.createCell(3).setCellValue(entry.getCategory().getName());
                    row.createCell(4).setCellValue(entry.getAccount().getName());
                    row.createCell(5).setCellValue(entry.getMemo() == null ? "" : entry.getMemo());
                    row.createCell(6).setCellValue(entry.getRecurringTransactionUid() == null ? "아니오" : "예");
                    var registeredCell = row.createCell(7);
                    if (entry.getRegTime() == null) registeredCell.setCellValue("");
                    else {
                        registeredCell.setCellValue(java.util.Date.from(entry.getRegTime()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant()));
                        registeredCell.setCellStyle(state.registeredStyle);
                    }
                    row.createCell(8).setCellValue(entry.getTransactionUid());
                }
            });
            workbook.write(output);
            output.flush();
            return count;
        } catch (IOException exception) {
            throw new IllegalStateException("XLSX export stream failed", exception);
        } finally {
            workbook.dispose();
        }
    }

    private long eachBatch(Long bookUid, TransactionSearchRequest filters, BatchConsumer consumer) throws IOException {
        java.time.LocalDate afterDate = null;
        Long afterUid = null;
        long count = 0;
        while (true) {
            List<MoneyBookTransaction> batch = transactions.findNextBatch(bookUid, filters,
                    afterDate, afterUid, BATCH_SIZE);
            if (batch.isEmpty()) return count;
            consumer.accept(batch);
            count += batch.size();
            MoneyBookTransaction last = batch.getLast();
            afterDate = last.getTransactionDate();
            afterUid = last.getTransactionUid();
            if (batch.size() < BATCH_SIZE) return count;
        }
    }

    private void writeRow(Writer writer, String[] columns) throws IOException {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) writer.write(',');
            String value = i >= 3 && i <= 5 ? safeText(columns[i]) : columns[i];
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write("\r\n");
    }

    /** Text cells beginning with spreadsheet formula operators are prefixed with an apostrophe. */
    private String safeText(String value) {
        int index = 0;
        while (index < value.length() && (Character.isWhitespace(value.charAt(index)) || value.charAt(index) < 0x20)) index++;
        if (index < value.length() && "=+-@".indexOf(value.charAt(index)) >= 0) return "'" + value;
        return value;
    }

    private final class SheetState {
        private final Workbook workbook;
        private final CellStyle headerStyle;
        private final CellStyle dateStyle;
        private final CellStyle registeredStyle;
        private Sheet sheet;
        private int sheetNumber;
        private int sheetRow;

        private SheetState(Workbook workbook, CellStyle headerStyle, CellStyle dateStyle, CellStyle registeredStyle) {
            this.workbook = workbook;
            this.headerStyle = headerStyle;
            this.dateStyle = dateStyle;
            this.registeredStyle = registeredStyle;
            nextSheet();
        }

        private void nextSheet() {
            sheetNumber++;
            sheet = workbook.createSheet(sheetNumber == 1 ? "거래내역" : "거래내역_" + sheetNumber);
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);
            int[] widths = {14, 12, 16, 20, 20, 40, 12, 24, 18};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheetRow = 1;
        }
    }

    @FunctionalInterface
    private interface BatchConsumer { void accept(List<MoneyBookTransaction> rows) throws IOException; }
}
