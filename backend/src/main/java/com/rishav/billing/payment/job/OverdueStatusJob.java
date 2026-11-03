package com.rishav.billing.payment.job;

import com.rishav.billing.invoice.entity.Invoice;
import com.rishav.billing.invoice.entity.InvoiceStatus;
import com.rishav.billing.invoice.repository.InvoiceRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueStatusJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueStatusJob.class);

    private final InvoiceRepository invoiceRepository;

    public OverdueStatusJob(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    @SchedulerLock(name = "overdue-status-job", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    @Transactional
    public void updateOverdueStatuses() {
        log.info("Running overdue status update job");

        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusInAndDueDateBefore(
                List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID), today);

        int count = 0;
        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            count++;
        }

        log.info("Overdue status job completed. Updated {} invoices", count);
    }
}
