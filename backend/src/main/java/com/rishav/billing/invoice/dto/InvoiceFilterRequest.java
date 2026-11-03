package com.rishav.billing.invoice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record InvoiceFilterRequest(
    String status,
    UUID customerId,
    LocalDate from,
    LocalDate to
) {}
