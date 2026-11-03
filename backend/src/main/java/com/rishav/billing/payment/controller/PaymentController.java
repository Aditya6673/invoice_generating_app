package com.rishav.billing.payment.controller;

import com.rishav.billing.common.dto.ApiResponse;
import com.rishav.billing.payment.dto.PaymentDto;
import com.rishav.billing.payment.dto.RecordPaymentRequest;
import com.rishav.billing.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/invoices/{invoiceId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody RecordPaymentRequest request) {
        PaymentDto dto = paymentService.recordPayment(invoiceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dto, "Payment recorded successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getPayments(@PathVariable UUID invoiceId) {
        List<PaymentDto> payments = paymentService.getPaymentsForInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }

    @PostMapping("/{paymentId}/reverse")
    public ResponseEntity<ApiResponse<PaymentDto>> reversePayment(
            @PathVariable UUID invoiceId,
            @PathVariable UUID paymentId,
            @RequestParam(required = false, defaultValue = "Manual reversal") String reason) {
        PaymentDto dto = paymentService.reversePayment(invoiceId, paymentId, reason);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Payment reversed successfully"));
    }
}
