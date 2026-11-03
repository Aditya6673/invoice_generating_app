package com.rishav.billing.reminder.job;

import com.rishav.billing.reminder.service.ReminderService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;
    private final boolean reminderEnabled;

    public ReminderScheduler(ReminderService reminderService,
                             @Value("${app.reminder.enabled:true}") boolean reminderEnabled) {
        this.reminderService = reminderService;
        this.reminderEnabled = reminderEnabled;
    }

    @Scheduled(cron = "${app.reminder.cron}")
    @SchedulerLock(name = "reminder-job", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void processReminders() {
        if (!reminderEnabled) {
            log.info("Reminder job execution skipped (app.reminder.enabled is false)");
            return;
        }

        try {
            log.info("Scheduled reminder job started");
            reminderService.processReminders();
            log.info("Scheduled reminder job finished successfully");
        } catch (Exception e) {
            log.error("Exception occurred during scheduled reminder job execution: {}", e.getMessage(), e);
        }
    }
}
