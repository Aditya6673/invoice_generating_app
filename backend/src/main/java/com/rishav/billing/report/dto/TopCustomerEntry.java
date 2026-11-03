package com.rishav.billing.report.dto;

import java.math.BigDecimal;

public record TopCustomerEntry(
    String customerName,
    long invoiceCount,
    BigDecimal totalRevenue
) {}
