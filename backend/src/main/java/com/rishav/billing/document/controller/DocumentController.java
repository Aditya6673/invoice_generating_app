package com.rishav.billing.document.controller;

import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.auth.repository.BusinessRepository;
import com.rishav.billing.common.dto.ApiResponse;
import com.rishav.billing.common.exception.BusinessRuleException;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.SecurityContextHelper;
import com.rishav.billing.customer.entity.Customer;
import com.rishav.billing.customer.repository.CustomerRepository;
import com.rishav.billing.document.service.PdfGenerationService;
import com.rishav.billing.invoice.entity.Invoice;
import com.rishav.billing.invoice.entity.InvoiceStatus;
import com.rishav.billing.invoice.repository.InvoiceRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;

@RestController
@RequestMapping("/invoices/{id}")
public class DocumentController {

    private final InvoiceRepository invoiceRepository;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final PdfGenerationService pdfGenerationService;
    private final JavaMailSender mailSender;

    public DocumentController(InvoiceRepository invoiceRepository,
                              BusinessRepository businessRepository,
                              CustomerRepository customerRepository,
                              PdfGenerationService pdfGenerationService,
                              JavaMailSender mailSender) {
        this.invoiceRepository = invoiceRepository;
        this.businessRepository = businessRepository;
        this.customerRepository = customerRepository;
        this.pdfGenerationService = pdfGenerationService;
        this.mailSender = mailSender;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessIdWithLines(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("PDF_DRAFT", "Cannot generate PDF for a draft invoice. Issue it first.");
        }

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        byte[] pdf = pdfGenerationService.generateInvoicePdf(invoice, business);

        String filename = (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "invoice")
                .replace("/", "-") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<String>> emailInvoice(@PathVariable UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Invoice invoice = invoiceRepository.findByIdAndBusinessIdWithLines(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("EMAIL_DRAFT", "Cannot email a draft invoice. Issue it first.");
        }

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        byte[] pdf = pdfGenerationService.generateInvoicePdf(invoice, business);
        String filename = (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "invoice")
                .replace("/", "-") + ".pdf";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Customer email from snapshot or we'd need to look it up
            // For now, we require the customer email to have been snapshotted or passed
            String recipientEmail = findCustomerEmail(invoice);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                throw new BusinessRuleException("NO_EMAIL",
                        "Customer does not have an email address on file");
            }

            helper.setTo(recipientEmail);
            helper.setSubject("Invoice " + invoice.getInvoiceNumber() + " from " + business.getName());
            helper.setText(String.format(
                    "Dear %s,\n\nPlease find attached invoice %s for %s %s.\n\nDue date: %s\n\nRegards,\n%s",
                    invoice.getCustomerName(),
                    invoice.getInvoiceNumber(),
                    invoice.getCurrency(),
                    invoice.getGrandTotal(),
                    invoice.getDueDate(),
                    business.getName()
            ));
            helper.addAttachment(filename, () -> new java.io.ByteArrayInputStream(pdf),
                    "application/pdf");

            mailSender.send(message);

            return ResponseEntity.ok(ApiResponse.ok("Invoice emailed successfully to " + recipientEmail));
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String findCustomerEmail(Invoice invoice) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        return customerRepository.findByIdAndBusinessId(invoice.getCustomerId(), businessId)
                .map(Customer::getEmail)
                .orElse(null);
    }
}
