package com.rishav.billing.reminder.repository;

import com.rishav.billing.reminder.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, UUID> {

    List<ReminderLog> findByInvoiceId(UUID invoiceId);

    List<ReminderLog> findByInvoiceIdOrderBySentAtDesc(UUID invoiceId);

    boolean existsByInvoiceIdAndMilestone(UUID invoiceId, String milestone);
}
