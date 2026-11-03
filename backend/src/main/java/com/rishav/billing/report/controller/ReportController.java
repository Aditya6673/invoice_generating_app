package com.rishav.billing.report.controller;

import com.rishav.billing.common.dto.ApiResponse;
import com.rishav.billing.report.dto.*;
import com.rishav.billing.report.service.ExportService;
import com.rishav.billing.report.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT')")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;

    public ReportController(ReportService reportService, ExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryReport>> getSummary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        SummaryReport report = reportService.getSummary(from, to);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    @GetMapping("/monthly-sales")
    public ResponseEntity<ApiResponse<List<MonthlySalesEntry>>> getMonthlySales(
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) {
            year = LocalDate.now().getYear();
        }
        List<MonthlySalesEntry> entries = reportService.getMonthlySales(year);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @GetMapping("/top-customers")
    public ResponseEntity<ApiResponse<List<TopCustomerEntry>>> getTopCustomers(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        List<TopCustomerEntry> entries = reportService.getTopCustomers(from, to, limit);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInvoices(
            @RequestParam(defaultValue = "csv") String type,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {

        List<InvoiceExportRow> data = reportService.getExportData(from, to);

        if ("xlsx".equalsIgnoreCase(type)) {
            byte[] xlsx = exportService.exportInvoicesXlsx(data);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoices.xlsx\"")
                    .body(xlsx);
        } else {
            byte[] csv = exportService.exportInvoicesCsv(data);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoices.csv\"")
                    .body(csv);
        }
    }
}
