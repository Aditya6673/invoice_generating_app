package com.rishav.billing.invoice.controller;

import com.rishav.billing.common.dto.ApiResponse;
import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.invoice.dto.*;
import com.rishav.billing.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDto>> createDraft(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceDto dto = invoiceService.createDraft(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Draft invoice created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InvoiceDto>>> listInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        InvoiceFilterRequest filters = new InvoiceFilterRequest(status, customerId, from, to);
        PageResponse<InvoiceDto> page = invoiceService.listInvoices(filters, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(@PathVariable UUID id) {
        InvoiceDto dto = invoiceService.getInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        InvoiceDto dto = invoiceService.updateDraft(id, request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Draft invoice updated"));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<ApiResponse<IssueInvoiceResponse>> issueInvoice(@PathVariable UUID id) {
        IssueInvoiceResponse response = invoiceService.issueInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Invoice issued successfully"));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<ApiResponse<InvoiceDto>> voidInvoice(@PathVariable UUID id) {
        InvoiceDto dto = invoiceService.voidInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Invoice voided"));
    }
}
