package com.rishav.billing.reminder.channel;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "app.notification.channel", havingValue = "email", matchIfMissing = true)
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailNotificationChannel(JavaMailSender mailSender,
                                    @Value("${spring.mail.username:noreply@billing.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public String send(String recipientEmail, String subject, String body, byte[] pdfAttachment, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (pdfAttachment != null && pdfAttachment.length > 0) {
                String filename = (attachmentName != null && !attachmentName.isBlank()) ? attachmentName : "invoice.pdf";
                helper.addAttachment(filename, new ByteArrayResource(pdfAttachment));
            }

            mailSender.send(message);
            log.info("Successfully sent reminder email to {}", recipientEmail);
            return "SENT";
        } catch (Exception e) {
            log.error("Failed to send reminder email to {}: {}", recipientEmail, e.getMessage(), e);
            return "FAILED";
        }
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }
}
