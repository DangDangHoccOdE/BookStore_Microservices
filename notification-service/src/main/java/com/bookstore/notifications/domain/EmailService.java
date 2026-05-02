package com.bookstore.notifications.domain;

import com.bookstore.notifications.ApplicationProperties;
import com.bookstore.notifications.domain.models.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final ApplicationProperties properties;

    public EmailService(JavaMailSender mailSender, ApplicationProperties applicationProperties) {
        this.mailSender = mailSender;
        this.properties = applicationProperties;
    }

    @Async("emailExecutor")
    public void sendEmail(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(properties.supportEmail());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.content());
            mailSender.send(mimeMessage);
            log.info("Email sent to: {}", message.to());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", message.to(), e);
            throw new RuntimeException("Error while sending email", e);
        }
    }
}
