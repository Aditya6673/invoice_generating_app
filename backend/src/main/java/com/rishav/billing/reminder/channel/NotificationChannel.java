package com.rishav.billing.reminder.channel;

public interface NotificationChannel {

    String send(String recipientEmail, String subject, String body, byte[] pdfAttachment, String attachmentName);

    String getChannelType();
}
