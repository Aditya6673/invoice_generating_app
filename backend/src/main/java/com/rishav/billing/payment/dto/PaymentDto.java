package com.rishav.billing.payment.dto;

import com.rishav.billing.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentDto(
    UUID id,
    UUID invoiceId,
    BigDecimal amount,
    LocalDate paymentDate,
    String method,
    String reference,
    boolean isReversal,
    String notes,
    Instant createdAt
) {
    public static PaymentDto from(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentDto(
            payment.getId(),
            payment.getInvoiceId(),
            payment.getAmount(),
            payment.getPaymentDate(),
            payment.getMethod() != null ? payment.getMethod().name() : null,
            payment.getReference(),
            payment.isReversal(),
            payment.getNotes(),
            payment.getCreatedAt()
        );
    }
}
