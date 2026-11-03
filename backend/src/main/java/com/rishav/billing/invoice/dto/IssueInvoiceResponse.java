package com.rishav.billing.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IssueInvoiceResponse(
    UUID id,
    String invoiceNumber,
    String status,
    LocalDate issueDate,
    LocalDate dueDate,
    BigDecimal grandTotal
) {}
