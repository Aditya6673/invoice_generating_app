package com.rishav.billing.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    UUID customerId,
    String customerName,
    String status,
    LocalDate issueDate,
    LocalDate dueDate,
    String currency,
    BigDecimal subtotal,
    BigDecimal discountTotal,
    BigDecimal taxTotal,
    BigDecimal grandTotal,
    BigDecimal amountPaid,
    String notes,
    List<InvoiceLineDto> lines,
    Instant createdAt
) {}
