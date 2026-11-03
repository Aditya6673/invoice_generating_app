package com.rishav.billing.reminder.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notification.channel", havingValue = "console")
public class ConsoleNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationChannel.class);

    @Override
    public String send(String recipientEmail, String subject, String body, byte[] pdfAttachment, String attachmentName) {
        int attachmentSize = (pdfAttachment != null) ? pdfAttachment.length : 0;
        log.info("""
            [CONSOLE NOTIFICATION]
            To: {}
            Subject: {}
            Attachment: {} ({} bytes)
            Body:
            {}
            """, recipientEmail, subject, attachmentName, attachmentSize, body);
        return "SENT";
    }

    @Override
    public String getChannelType() {
        return "CONSOLE";
    }
}
