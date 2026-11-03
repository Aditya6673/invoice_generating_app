package com.rishav.billing.payment.service;

import com.rishav.billing.invoice.entity.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class InvoiceStatusDeriver {

    public static InvoiceStatus derive(BigDecimal amountPaid, BigDecimal grandTotal, LocalDate dueDate) {
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal total = grandTotal != null ? grandTotal : BigDecimal.ZERO;

        if (paid.compareTo(total) >= 0) {
            return InvoiceStatus.PAID;
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
                return InvoiceStatus.OVERDUE;
            }
            return InvoiceStatus.PARTIALLY_PAID;
        }
        // amountPaid == 0
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return InvoiceStatus.OVERDUE;
        }
        return InvoiceStatus.ISSUED; // unpaid, not yet overdue — stays ISSUED
    }
}
