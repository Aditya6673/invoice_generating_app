package com.rishav.billing.document.service;

import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.invoice.entity.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Generates invoice PDFs from Thymeleaf XHTML templates using Flying Saucer.
 * The template data comes from the snapshot fields on the Invoice entity,
 * so changes to customer/product data after issuance don't affect old invoices.
 */
@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    private final TemplateEngine pdfTemplateEngine;

    public PdfGenerationService(@Qualifier("pdfTemplateEngine") TemplateEngine pdfTemplateEngine) {
        this.pdfTemplateEngine = pdfTemplateEngine;
    }

    public byte[] generateInvoicePdf(Invoice invoice, Business business) {
        Context ctx = new Context();
        ctx.setVariable("invoice", invoice);
        ctx.setVariable("business", business);
        ctx.setVariable("lines", invoice.getLines());

        boolean isIntraState = business.getStateCode() != null
                && invoice.getCustomerStateCode() != null
                && business.getStateCode().equals(invoice.getCustomerStateCode());
        ctx.setVariable("isIntraState", isIntraState);

        // Compute balance due
        ctx.setVariable("balanceDue",
                invoice.getGrandTotal().subtract(invoice.getAmountPaid()));

        String html = pdfTemplateEngine.process("invoice-template", ctx);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate PDF for invoice {}", invoice.getInvoiceNumber(), ex);
            throw new RuntimeException("Failed to generate PDF: " + ex.getMessage(), ex);
        }
    }
}
