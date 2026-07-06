package com.itsm.ticketing.service;

import com.itsm.ticketing.entity.Ticket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.from.address:no-reply@yourcompany.com}")
    private String fromAddress;

    @Value("${mail.from.name:ITSM Ticketing System}")
    private String fromName;

    /**
     * Send an email asynchronously when a new ticket is created.
     * Uses Thymeleaf template 'ticket-created.html'.
     *
     * @param toEmail The recipient email address
     * @param ticket  The ticket that was created
     */
    @Async
    public void sendTicketCreatedEmail(String toEmail, Ticket ticket) {
        try {
            log.info("Preparing to send Ticket Created email to: {}", toEmail);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            // Prepare template context
            Context context = new Context();
            context.setVariable("ticketNumber", ticket.getTicketNumber());
            context.setVariable("title", ticket.getTitle());
            context.setVariable("priority", ticket.getPriority().name());
            context.setVariable("status", ticket.getStatus().name());
            context.setVariable("requesterName", ticket.getRequester().getName());
            context.setVariable("clientName", ticket.getClient().getClientName());

            // Process HTML template
            String htmlContent = templateEngine.process("ticket-created", context);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("New Ticket Created: [" + ticket.getTicketNumber() + "] " + ticket.getTitle());
            helper.setText(htmlContent, true); // true indicates HTML content

            javaMailSender.send(message);
            log.info("Ticket Created email sent successfully to: {}", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
