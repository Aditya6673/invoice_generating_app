package com.rishav.billing.reminder.controller;

import com.rishav.billing.common.dto.ApiResponse;
import com.rishav.billing.reminder.dto.ReminderLogDto;
import com.rishav.billing.reminder.service.ReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/invoices/{invoiceId}/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ReminderLogDto>> sendReminder(@PathVariable UUID invoiceId) {
        ReminderLogDto reminderLog = reminderService.sendManualReminder(invoiceId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reminderLog, "Manual reminder sent successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ReminderLogDto>>> getReminderHistory(@PathVariable UUID invoiceId) {
        List<ReminderLogDto> history = reminderService.getReminderHistory(invoiceId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
