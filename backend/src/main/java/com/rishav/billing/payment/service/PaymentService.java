package com.rishav.billing.payment.service;

import com.rishav.billing.common.audit.AuditService;
import com.rishav.billing.common.exception.BusinessRuleException;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.SecurityContextHelper;
import com.rishav.billing.invoice.entity.Invoice;
import com.rishav.billing.invoice.entity.InvoiceStatus;
import com.rishav.billing.invoice.repository.InvoiceRepository;
import com.rishav.billing.payment.dto.PaymentDto;
import com.rishav.billing.payment.dto.RecordPaymentRequest;
import com.rishav.billing.payment.entity.Payment;
import com.rishav.billing.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditService = auditService;
    }

    public PaymentDto recordPayment(UUID invoiceId, RecordPaymentRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessId(invoiceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("PAYMENT_ON_DRAFT",
                    "Cannot record payment on a draft invoice. Issue it first.");
        }
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessRuleException("PAYMENT_ON_VOID",
                    "Cannot record payment on a voided invoice.");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT",
                    "Payment amount must be positive");
        }

        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setMethod(request.method());
        payment.setReference(request.reference());
        payment.setNotes(request.notes());
        payment.setRecordedBy(SecurityContextHelper.getCurrentUserId());
        payment.setReversal(false);

        Payment saved = paymentRepository.save(payment);

        // Update invoice amount_paid
        invoice.setAmountPaid(invoice.getAmountPaid().add(request.amount()));

        // Derive new status
        InvoiceStatus newStatus = InvoiceStatusDeriver.derive(
                invoice.getAmountPaid(), invoice.getGrandTotal(), invoice.getDueDate());
        invoice.setStatus(newStatus);

        invoiceRepository.save(invoice);

        auditService.log("INVOICE", invoiceId, "PAYMENT_RECORDED",
                String.format("Payment of %s recorded via %s. Total paid: %s / %s",
                        request.amount(), request.method(),
                        invoice.getAmountPaid(), invoice.getGrandTotal()));

        return toDto(saved);
    }

    public PaymentDto reversePayment(UUID invoiceId, UUID paymentId, String reason) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessId(invoiceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        Payment original = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!original.getInvoiceId().equals(invoiceId)) {
            throw new BusinessRuleException("PAYMENT_MISMATCH",
                    "Payment does not belong to this invoice");
        }

        if (original.isReversal()) {
            throw new BusinessRuleException("ALREADY_REVERSED",
                    "Cannot reverse a reversal entry");
        }

        // Create reversing entry with negated amount
        Payment reversal = new Payment();
        reversal.setInvoiceId(invoiceId);
        reversal.setAmount(original.getAmount().negate());
        reversal.setPaymentDate(original.getPaymentDate());
        reversal.setMethod(original.getMethod());
        reversal.setReference("REVERSAL of " + paymentId);
        reversal.setNotes(reason);
        reversal.setRecordedBy(SecurityContextHelper.getCurrentUserId());
        reversal.setReversal(true);

        Payment saved = paymentRepository.save(reversal);

        // Update invoice — subtract the original amount
        invoice.setAmountPaid(invoice.getAmountPaid().subtract(original.getAmount()));

        InvoiceStatus newStatus = InvoiceStatusDeriver.derive(
                invoice.getAmountPaid(), invoice.getGrandTotal(), invoice.getDueDate());
        invoice.setStatus(newStatus);

        invoiceRepository.save(invoice);

        auditService.log("INVOICE", invoiceId, "PAYMENT_REVERSED",
                String.format("Payment %s reversed. Reason: %s. New paid amount: %s",
                        paymentId, reason, invoice.getAmountPaid()));

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsForInvoice(UUID invoiceId) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        // Verify invoice belongs to business
        invoiceRepository.findByIdAndBusinessId(invoiceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        return paymentRepository.findByInvoiceIdOrderByCreatedAtAsc(invoiceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PaymentDto toDto(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMethod().name(),
                payment.getReference(),
                payment.isReversal(),
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }
}
