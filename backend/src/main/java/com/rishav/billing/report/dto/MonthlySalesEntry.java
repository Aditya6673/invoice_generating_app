package com.rishav.billing.report.dto;

import java.math.BigDecimal;

public record MonthlySalesEntry(
    String month,
    long count,
    BigDecimal total
) {
}
