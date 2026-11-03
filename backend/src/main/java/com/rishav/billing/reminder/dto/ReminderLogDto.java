package com.rishav.billing.reminder.dto;

import com.rishav.billing.reminder.entity.ReminderLog;

import java.time.Instant;
import java.util.UUID;

public record ReminderLogDto(
    UUID id,
    UUID invoiceId,
    String milestone,
    Instant sentAt,
    String channel,
    String result,
    String errorMessage
) {
    public static ReminderLogDto fromEntity(ReminderLog entity) {
        if (entity == null) {
            return null;
        }
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
