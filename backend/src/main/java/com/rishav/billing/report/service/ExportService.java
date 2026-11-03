package com.rishav.billing.report.service;

import com.rishav.billing.report.dto.InvoiceExportRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final String[] CSV_HEADERS = {
            "Invoice Number", "Customer", "Issue Date", "Due Date", "Status",
            "Subtotal", "Tax", "Total", "Paid", "Balance"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public byte[] exportInvoicesCsv(List<InvoiceExportRow> rows) {
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append(String.join(",", CSV_HEADERS)).append("\n");

        // Data rows
        for (InvoiceExportRow row : rows) {
            sb.append(csvEscape(row.invoiceNumber())).append(",");
            sb.append(csvEscape(row.customerName())).append(",");
            sb.append(formatDate(row.issueDate())).append(",");
            sb.append(formatDate(row.dueDate())).append(",");
            sb.append(csvEscape(row.status())).append(",");
            sb.append(formatMoney(row.subtotal())).append(",");
            sb.append(formatMoney(row.taxTotal())).append(",");
            sb.append(formatMoney(row.grandTotal())).append(",");
            sb.append(formatMoney(row.amountPaid())).append(",");
            sb.append(formatMoney(row.balance())).append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportInvoicesXlsx(List<InvoiceExportRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Invoices");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Money cell style
            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("#,##0.00"));

            // Date cell style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd"));

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < CSV_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(CSV_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int i = 0; i < rows.size(); i++) {
                InvoiceExportRow data = rows.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(nullSafe(data.invoiceNumber()));
                row.createCell(1).setCellValue(nullSafe(data.customerName()));

                Cell issueDateCell = row.createCell(2);
                if (data.issueDate() != null) {
                    issueDateCell.setCellValue(data.issueDate().toString());
                }

                Cell dueDateCell = row.createCell(3);
                if (data.dueDate() != null) {
                    dueDateCell.setCellValue(data.dueDate().toString());
                }

                row.createCell(4).setCellValue(nullSafe(data.status()));

                setMoneyCell(row, 5, data.subtotal(), moneyStyle);
                setMoneyCell(row, 6, data.taxTotal(), moneyStyle);
                setMoneyCell(row, 7, data.grandTotal(), moneyStyle);
                setMoneyCell(row, 8, data.amountPaid(), moneyStyle);
                setMoneyCell(row, 9, data.balance(), moneyStyle);
            }

            // Auto-size columns
            for (int i = 0; i < CSV_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate XLSX export", e);
        }
    }

    private void setMoneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    private String formatMoney(BigDecimal value) {
        return value != null ? value.toPlainString() : "0.00";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
