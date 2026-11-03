package com.rishav.billing.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceExportRow(
    String invoiceNumber,
    String customerName,
    LocalDate issueDate,
    LocalDate dueDate,
    String status,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal grandTotal,
    BigDecimal amountPaid,
    BigDecimal balance
) {}
