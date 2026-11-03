package com.rishav.billing.reminder.service;

import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.auth.repository.BusinessRepository;
import com.rishav.billing.common.audit.AuditService;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.customer.entity.Customer;
import com.rishav.billing.customer.repository.CustomerRepository;
import com.rishav.billing.document.service.PdfGenerationService;
import com.rishav.billing.invoice.entity.Invoice;
import com.rishav.billing.invoice.entity.InvoiceStatus;
import com.rishav.billing.invoice.repository.InvoiceRepository;
import com.rishav.billing.reminder.channel.NotificationChannel;
import com.rishav.billing.reminder.dto.ReminderLogDto;
import com.rishav.billing.reminder.entity.BusinessReminderSettings;
import com.rishav.billing.reminder.entity.ReminderLog;
import com.rishav.billing.reminder.repository.BusinessReminderSettingsRepository;
import com.rishav.billing.reminder.repository.ReminderLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderLogRepository reminderLogRepository;
    private final BusinessReminderSettingsRepository settingsRepository;
    private final NotificationChannel notificationChannel;
    private final PdfGenerationService pdfGenerationService;
    private final InvoiceRepository invoiceRepository;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public ReminderService(ReminderLogRepository reminderLogRepository,
                           BusinessReminderSettingsRepository settingsRepository,
                           NotificationChannel notificationChannel,
                           PdfGenerationService pdfGenerationService,
                           InvoiceRepository invoiceRepository,
                           BusinessRepository businessRepository,
                           CustomerRepository customerRepository,
                           AuditService auditService) {
        this.reminderLogRepository = reminderLogRepository;
        this.settingsRepository = settingsRepository;
        this.notificationChannel = notificationChannel;
        this.pdfGenerationService = pdfGenerationService;
        this.invoiceRepository = invoiceRepository;
        this.businessRepository = businessRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    /**
     * Main reminder processing method called by the background scheduler.
     * Evaluates reminder milestones for all businesses with reminders enabled.
     */
    public void processReminders() {
        log.info("Starting automated reminder processing");
        List<BusinessReminderSettings> activeSettings = settingsRepository.findByEnabledTrue();
        LocalDate today = LocalDate.now();

        for (BusinessReminderSettings settings : activeSettings) {
            try {
                processBusinessReminders(settings, today);
            } catch (Exception e) {
                log.error("Error processing reminders for business {}: {}",
                        settings.getBusinessId(), e.getMessage(), e);
            }
        }
        log.info("Completed automated reminder processing");
    }

    /**
     * Send an ad-hoc reminder for a specific invoice immediately.
     */
    public ReminderLogDto sendManualReminder(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        Business business = businessRepository.findById(invoice.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", invoice.getBusinessId()));

        ReminderLog saved = sendNotification(invoice, business, "MANUAL");
        return toDto(saved);
    }

    /**
     * Retrieve reminder history for an invoice.
     */
    @Transactional(readOnly = true)
    public List<ReminderLogDto> getReminderHistory(UUID invoiceId) {
        return reminderLogRepository.findByInvoiceId(invoiceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ─── Internal ───────────────────────────────────────────────────────

    private void processBusinessReminders(BusinessReminderSettings settings, LocalDate today) {
        UUID businessId = settings.getBusinessId();
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) return;

        log.debug("Processing reminders for business: {} ({})", business.getName(), businessId);

        // 1. Invoices due in beforeDueDays days
        int beforeDays = settings.getBeforeDueDays();
        if (beforeDays > 0) {
            LocalDate targetDate = today.plusDays(beforeDays);
            processForDueDate(business, targetDate, "DUE_IN_" + beforeDays + "_DAYS");
        }

        // 2. Invoices due today
        processForDueDate(business, today, "DUE_TODAY");

        // 3. Overdue intervals
        List<Integer> intervals = parseOverdueIntervals(settings.getOverdueIntervals());
        for (int daysOverdue : intervals) {
            LocalDate overdueDate = today.minusDays(daysOverdue);
            processForDueDate(business, overdueDate, "OVERDUE_" + daysOverdue);
        }
    }

    private void processForDueDate(Business business, LocalDate dueDate, String milestone) {
        // Find unpaid invoices with this due date
        List<Invoice> invoices = invoiceRepository.findByStatusInAndDueDateBefore(
                List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE),
                dueDate.plusDays(1) // inclusive
        );

        for (Invoice invoice : invoices) {
            // Filter to this business and exact due date
            if (!invoice.getBusinessId().equals(business.getId())) continue;
            if (invoice.getDueDate() == null || !invoice.getDueDate().equals(dueDate)) continue;

            // Skip if already reminded for this milestone
            if (reminderLogRepository.existsByInvoiceIdAndMilestone(invoice.getId(), milestone)) {
                continue;
            }

            sendNotification(invoice, business, milestone);
        }
    }

    private ReminderLog sendNotification(Invoice invoice, Business business, String milestone) {
        // Look up customer email
        String recipientEmail = customerRepository.findById(invoice.getCustomerId())
                .map(Customer::getEmail)
                .orElse(null);

        byte[] pdfAttachment = null;
        try {
            pdfAttachment = pdfGenerationService.generateInvoicePdf(invoice, business);
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}", invoice.getId(), e);
        }

        String result = "FAILED";
        String errorMessage = null;

        if (recipientEmail == null || recipientEmail.isBlank()) {
            errorMessage = "No email address for customer of invoice " + invoice.getInvoiceNumber();
            log.warn(errorMessage);
        } else {
            try {
                BigDecimal balance = invoice.getGrandTotal().subtract(invoice.getAmountPaid());
                String subject = buildSubject(invoice, business.getName(), milestone);
                String body = buildBody(invoice, business.getName(), balance, milestone);
                String attachmentName = (invoice.getInvoiceNumber() != null
                        ? invoice.getInvoiceNumber().replace("/", "-") : "invoice") + ".pdf";

                result = notificationChannel.send(recipientEmail, subject, body, pdfAttachment, attachmentName);
            } catch (Exception e) {
                log.error("Failed to send reminder for invoice {}", invoice.getId(), e);
                errorMessage = e.getMessage();
            }
        }

        ReminderLog entry = new ReminderLog();
        entry.setInvoiceId(invoice.getId());
        entry.setMilestone(milestone);
        entry.setSentAt(Instant.now());
        entry.setChannel(notificationChannel.getChannelType());
        entry.setResult(result);
        entry.setErrorMessage(errorMessage);

        ReminderLog saved = reminderLogRepository.save(entry);

        auditService.log("INVOICE", invoice.getId(), "REMINDER_SENT",
                "Milestone: " + milestone + ", Channel: " + notificationChannel.getChannelType()
                        + ", Result: " + result);

        return saved;
    }

    private String buildSubject(Invoice invoice, String businessName, String milestone) {
        String num = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "Invoice";
        return switch (milestone) {
            case "DUE_TODAY" -> "Payment Due Today: " + num + " — " + businessName;
            case "MANUAL" -> "Invoice Reminder: " + num + " — " + businessName;
            default -> {
                if (milestone.startsWith("OVERDUE_"))
                    yield "Overdue Notice: " + num + " — " + businessName;
                else if (milestone.startsWith("DUE_IN_"))
                    yield "Upcoming Payment Reminder: " + num + " — " + businessName;
                else yield "Invoice Reminder: " + num + " — " + businessName;
            }
        };
    }

    private String buildBody(Invoice invoice, String businessName, BigDecimal balance, String milestone) {
        String customerName = invoice.getCustomerName() != null ? invoice.getCustomerName() : "Valued Customer";
        String invNum = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "";

        String milestoneText = switch (milestone) {
            case "DUE_TODAY" -> "Your payment is due today.";
            case "MANUAL" -> "This is a reminder regarding your invoice.";
            default -> {
                if (milestone.startsWith("OVERDUE_")) {
                    String days = milestone.replace("OVERDUE_", "");
                    yield "Your payment is overdue by " + days + " day(s).";
                } else if (milestone.startsWith("DUE_IN_")) {
                    String days = milestone.replace("DUE_IN_", "").replace("_DAYS", "");
                    yield "Your payment is due in " + days + " day(s).";
                } else yield "This is a payment reminder.";
            }
        };

        return String.format("""
                Dear %s,

                This is regarding Invoice %s dated %s for %s %s.

                %s

                Balance due: %s %s
                Due date: %s

                Please find the invoice attached.

                Regards,
                %s
                """,
                customerName, invNum, invoice.getIssueDate(),
                invoice.getCurrency(), invoice.getGrandTotal(),
                milestoneText,
                invoice.getCurrency(), balance,
                invoice.getDueDate(),
                businessName);
    }

    private List<Integer> parseOverdueIntervals(String intervals) {
        if (intervals == null || intervals.isBlank()) {
            return List.of(1, 7, 15);
        }
        List<Integer> list = new ArrayList<>();
        for (String item : intervals.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                try {
                    list.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return list.isEmpty() ? List.of(1, 7, 15) : list;
    }

    private ReminderLogDto toDto(ReminderLog entity) {
        return new ReminderLogDto(
                entity.getId(),
                entity.getInvoiceId(),
                entity.getMilestone(),
                entity.getSentAt(),
                entity.getChannel(),
                entity.getResult(),
                entity.getErrorMessage()
        );
    }
}
