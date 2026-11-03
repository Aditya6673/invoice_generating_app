package com.rishav.billing.invoice.service;

import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.auth.repository.BusinessRepository;
import com.rishav.billing.common.audit.AuditService;
import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.common.exception.BusinessRuleException;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.SecurityContextHelper;
import com.rishav.billing.customer.entity.Customer;
import com.rishav.billing.customer.repository.CustomerRepository;
import com.rishav.billing.invoice.dto.*;
import com.rishav.billing.invoice.entity.Invoice;
import com.rishav.billing.invoice.entity.InvoiceLine;
import com.rishav.billing.invoice.entity.InvoiceStatus;
import com.rishav.billing.invoice.repository.InvoiceRepository;
import com.rishav.billing.invoice.tax.TaxCalculator;
import com.rishav.billing.invoice.tax.TaxLineResult;
import com.rishav.billing.invoice.tax.TaxResult;
import com.rishav.billing.invoice.tax.TaxableLineItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final InvoiceNumberGenerator numberGenerator;
    private final TaxCalculator taxCalculator;
    private final AuditService auditService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          BusinessRepository businessRepository,
                          InvoiceNumberGenerator numberGenerator,
                          TaxCalculator taxCalculator,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.businessRepository = businessRepository;
        this.numberGenerator = numberGenerator;
        this.taxCalculator = taxCalculator;
        this.auditService = auditService;
    }

    public InvoiceDto createDraft(CreateInvoiceRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        // Validate customer exists and belongs to this business
        Customer customer = customerRepository.findByIdAndBusinessId(request.customerId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.customerId()));

        Business business = getBusinessOrThrow(businessId);

        Invoice invoice = new Invoice();
        invoice.setBusinessId(businessId);
        invoice.setCustomerId(request.customerId());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCurrency(request.currency());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());

        // Map line items
        mapLinesToInvoice(invoice, request.lines());

        // Calculate tax
        recalculateTotals(invoice, business.getStateCode(), customer.getStateCode());

        Invoice saved = invoiceRepository.save(invoice);
        return toDto(saved);
    }

    public InvoiceDto updateDraft(UUID id, UpdateInvoiceRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (!invoice.isEditable()) {
            throw new BusinessRuleException("INVOICE_NOT_EDITABLE",
                    "Only DRAFT invoices can be edited. Current status: " + invoice.getStatus());
        }

        Business business = getBusinessOrThrow(businessId);

        if (request.customerId() != null) {
            customerRepository.findByIdAndBusinessId(request.customerId(), businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.customerId()));
            invoice.setCustomerId(request.customerId());
        }

        if (request.dueDate() != null) {
            invoice.setDueDate(request.dueDate());
        }
        if (request.notes() != null) {
            invoice.setNotes(request.notes());
        }

        if (request.lines() != null && !request.lines().isEmpty()) {
            invoice.getLines().clear();
            mapLinesToInvoice(invoice, request.lines());
        }

        // Recalculate
        Customer customer = customerRepository.findByIdAndBusinessId(invoice.getCustomerId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", invoice.getCustomerId()));
        recalculateTotals(invoice, business.getStateCode(), customer.getStateCode());

        Invoice saved = invoiceRepository.save(invoice);
        return toDto(saved);
    }

    public IssueInvoiceResponse issueInvoice(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("INVOICE_ALREADY_ISSUED",
                    "Invoice is already issued with status: " + invoice.getStatus());
        }

        if (invoice.getLines().isEmpty()) {
            throw new BusinessRuleException("INVOICE_NO_LINES",
                    "Cannot issue an invoice with no line items");
        }

        Business business = getBusinessOrThrow(businessId);
        Customer customer = customerRepository.findByIdAndBusinessId(invoice.getCustomerId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", invoice.getCustomerId()));

        // Snapshot customer details
        invoice.setCustomerName(customer.getName());
        invoice.setCustomerAddress(formatCustomerAddress(customer));
        invoice.setCustomerState(customer.getState());
        invoice.setCustomerStateCode(customer.getStateCode());
        invoice.setCustomerGstin(customer.getGstin());

        // Final tax recalculation
        recalculateTotals(invoice, business.getStateCode(), customer.getStateCode());

        // Generate gap-free invoice number
        String invoiceNumber = numberGenerator.generateNumber(businessId, business.getInvoicePrefix());
        invoice.setInvoiceNumber(invoiceNumber);

        // Set status and dates
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(LocalDate.now());
        if (invoice.getDueDate() == null) {
            int terms = business.getDefaultPaymentTerms() != null ? business.getDefaultPaymentTerms() : 30;
            invoice.setDueDate(LocalDate.now().plusDays(terms));
        }

        Invoice saved = invoiceRepository.save(invoice);

        auditService.log("INVOICE", saved.getId(), "ISSUED",
                "Invoice " + invoiceNumber + " issued for " + saved.getGrandTotal());

        return new IssueInvoiceResponse(
                saved.getId(),
                saved.getInvoiceNumber(),
                saved.getStatus().name(),
                saved.getIssueDate(),
                saved.getDueDate(),
                saved.getGrandTotal()
        );
    }

    public InvoiceDto voidInvoice(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("CANNOT_VOID_DRAFT", "Delete the draft instead of voiding it");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("CANNOT_VOID_PAID",
                    "Cannot void a fully paid invoice. Record a credit note instead.");
        }
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessRuleException("ALREADY_VOID", "Invoice is already voided");
        }

        invoice.setStatus(InvoiceStatus.VOID);
        Invoice saved = invoiceRepository.save(invoice);

        auditService.log("INVOICE", saved.getId(), "VOIDED",
                "Invoice " + saved.getInvoiceNumber() + " voided");

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Invoice invoice = invoiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return toDto(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDto> listInvoices(InvoiceFilterRequest filters, Pageable pageable) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Page<Invoice> page;
        if (filters != null && filters.status() != null && !filters.status().isBlank()) {
            InvoiceStatus status = InvoiceStatus.valueOf(filters.status().toUpperCase());
            page = invoiceRepository.findByBusinessIdAndStatus(businessId, status, pageable);
        } else if (filters != null && filters.customerId() != null) {
            page = invoiceRepository.findByBusinessIdAndCustomerId(businessId, filters.customerId(), pageable);
        } else {
            page = invoiceRepository.findByBusinessId(businessId, pageable);
        }

        return PageResponse.from(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDto> getInvoicesByCustomer(UUID customerId, Pageable pageable) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Page<Invoice> page = invoiceRepository.findByBusinessIdAndCustomerId(businessId, customerId, pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    /**
     * Get the raw Invoice entity — for use by other modules (payment, document, reminder).
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceEntity(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        return invoiceRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
    }

    // ─── Internal helpers ───────────────────────────────────────────────

    private void recalculateTotals(Invoice invoice, String businessStateCode, String customerStateCode) {
        List<TaxableLineItem> taxableItems = invoice.getLines().stream()
                .map(line -> new TaxableLineItem(
                        line.getDescription(),
                        line.getHsnSacCode(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getDiscountAmount(),
                        line.getTaxRate()
                ))
                .toList();

        TaxResult result = taxCalculator.calculate(businessStateCode, customerStateCode, taxableItems);

        // Update each line with computed tax amounts
        List<InvoiceLine> lines = invoice.getLines();
        List<TaxLineResult> lineResults = result.lineResults();
        for (int i = 0; i < lines.size(); i++) {
            InvoiceLine line = lines.get(i);
            TaxLineResult taxLine = lineResults.get(i);
            line.setTaxableAmount(taxLine.taxableAmount());
            line.setCgstAmount(taxLine.cgstAmount());
            line.setSgstAmount(taxLine.sgstAmount());
            line.setIgstAmount(taxLine.igstAmount());
            line.setLineTotal(taxLine.lineTotal());
        }

        // Update invoice totals
        invoice.setSubtotal(result.subtotal());
        invoice.setDiscountTotal(result.discountTotal());
        invoice.setTaxTotal(result.taxTotal());
        invoice.setGrandTotal(result.grandTotal());
    }

    private void mapLinesToInvoice(Invoice invoice, List<InvoiceLineRequest> lineRequests) {
        for (int i = 0; i < lineRequests.size(); i++) {
            InvoiceLineRequest req = lineRequests.get(i);
            InvoiceLine line = new InvoiceLine();
            line.setDescription(req.description());
            line.setHsnSacCode(req.hsnSacCode());
            line.setQuantity(req.quantity());
            line.setUnitPrice(req.unitPrice());
            line.setDiscountAmount(req.discountAmount());
            line.setTaxRate(req.taxRate());
            line.setSortOrder(i);
            invoice.addLine(line);
        }
    }

    private String formatCustomerAddress(Customer customer) {
        StringBuilder sb = new StringBuilder();
        if (customer.getBillingAddress() != null) sb.append(customer.getBillingAddress());
        if (customer.getCity() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(customer.getCity());
        }
        if (customer.getState() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(customer.getState());
        }
        if (customer.getPincode() != null) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(customer.getPincode());
        }
        return sb.toString();
    }

    private Business getBusinessOrThrow(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
    }

    private InvoiceDto toDto(Invoice invoice) {
        List<InvoiceLineDto> lineDtos = invoice.getLines().stream()
                .map(line -> new InvoiceLineDto(
                        line.getId(),
                        line.getDescription(),
                        line.getHsnSacCode(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getDiscountAmount(),
                        line.getTaxRate(),
                        line.getTaxableAmount(),
                        line.getCgstAmount(),
                        line.getSgstAmount(),
                        line.getIgstAmount(),
                        line.getLineTotal()
                ))
                .toList();

        return new InvoiceDto(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomerId(),
                invoice.getCustomerName(),
                invoice.getStatus().name(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getDiscountTotal(),
                invoice.getTaxTotal(),
                invoice.getGrandTotal(),
                invoice.getAmountPaid(),
                invoice.getNotes(),
                lineDtos,
                invoice.getCreatedAt()
        );
    }
}
