package com.rishav.billing.report.dto;

import java.math.BigDecimal;

public record SummaryReport(
    long invoiceCount,
    BigDecimal totalSales,
    BigDecimal pendingAmount,
    BigDecimal overdueAmount,
    BigDecimal totalCollected
) {
}
