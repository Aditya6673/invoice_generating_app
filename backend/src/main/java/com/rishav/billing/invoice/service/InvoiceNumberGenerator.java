package com.rishav.billing.invoice.service;

import com.rishav.billing.invoice.entity.InvoiceSequence;
import com.rishav.billing.invoice.repository.InvoiceSequenceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Generates gap-free sequential invoice numbers per business per financial year.
 * Must be called within the same @Transactional as the invoice issue operation.
 * Uses SELECT ... FOR UPDATE (via @Lock(PESSIMISTIC_WRITE)) to prevent concurrent
 * access from creating gaps or duplicates.
 */
@Component
public class InvoiceNumberGenerator {

    private final InvoiceSequenceRepository sequenceRepository;

    public InvoiceNumberGenerator(InvoiceSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Generate the next invoice number for the given business.
     * Format: {prefix}/{YYYY-YY}/{zero-padded 4-digit number}
     * Example: INV/2026-27/0001
     */
    public String generateNumber(UUID businessId, String invoicePrefix) {
        String financialYear = getFinancialYear(LocalDate.now());

        InvoiceSequence sequence = sequenceRepository
                .findForUpdate(businessId, financialYear)
                .orElseGet(() -> {
                    InvoiceSequence newSeq = new InvoiceSequence();
                    newSeq.setBusinessId(businessId);
                    newSeq.setFinancialYear(financialYear);
                    newSeq.setLastNumber(0);
                    return sequenceRepository.save(newSeq);
                });

        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        sequenceRepository.save(sequence);

        String prefix = (invoicePrefix != null && !invoicePrefix.isBlank()) ? invoicePrefix : "INV";
        return String.format("%s/%s/%04d", prefix, financialYear, nextNumber);
    }

    /**
     * Determines the Indian financial year (April–March) for a given date.
     * Nov 2026 -> "2026-27", Feb 2027 -> "2026-27"
     */
    public String getFinancialYear(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        if (month >= 4) {
            // April onwards: current year to next year
            return String.format("%d-%02d", year, (year + 1) % 100);
        } else {
            // Jan–Mar: previous year to current year
            return String.format("%d-%02d", year - 1, year % 100);
        }
    }
}
