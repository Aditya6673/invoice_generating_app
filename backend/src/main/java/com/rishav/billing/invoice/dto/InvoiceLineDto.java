package com.rishav.billing.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineDto(
    UUID id,
    String description,
    String hsnSacCode,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal discountAmount,
    BigDecimal taxRate,
    BigDecimal taxableAmount,
    BigDecimal cgstAmount,
    BigDecimal sgstAmount,
    BigDecimal igstAmount,
    BigDecimal lineTotal
) {}
