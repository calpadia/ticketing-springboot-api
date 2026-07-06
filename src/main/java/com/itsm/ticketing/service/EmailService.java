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

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${mail.from.name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

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

            String ticketUrl = frontendUrl + "/tickets/" + ticket.getId();

            // Prepare template context
            Context context = new Context();
            context.setVariable("ticketNumber", ticket.getTicketNumber());
            context.setVariable("title", ticket.getTitle());
            context.setVariable("priority", ticket.getPriority().name());
            context.setVariable("status", ticket.getStatus().name());
            context.setVariable("requesterName", ticket.getRequester().getName());
            context.setVariable("clientName", ticket.getClient().getCompanyName());
            context.setVariable("ticketUrl", ticketUrl);

            // Process HTML template
            String htmlContent = templateEngine.process("ticket-created", context);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("New Ticket Created: [" + ticket.getTicketNumber() + "] " + ticket.getTitle());
            
            String plainText = String.format("Hello,\n\nA new ticket has been created.\nTicket Number: %s\nTitle: %s\nPriority: %s\nStatus: %s\nRequester: %s\nClient: %s\n\nView Ticket: %s",
                    ticket.getTicketNumber(), ticket.getTitle(), ticket.getPriority().name(), ticket.getStatus().name(), ticket.getRequester().getName(), ticket.getClient().getCompanyName(), ticketUrl);
            
            helper.setText(plainText, htmlContent); // first arg is plain text, second arg is html

            javaMailSender.send(message);
            log.info("Ticket Created email sent successfully to: {}", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Send an email asynchronously when a ticket is assigned to a support engineer.
     * Uses Thymeleaf template 'ticket-assigned.html'.
     *
     * @param toEmail The recipient email address (assignee)
     * @param ticket  The ticket that was assigned
     * @param assignerName The name of the user who assigned the ticket
     */
    @Async
    public void sendTicketAssignedEmail(String toEmail, Ticket ticket, String assignerName) {
        try {
            log.info("Preparing to send Ticket Assigned email to: {}", toEmail);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String ticketUrl = frontendUrl + "/tickets/" + ticket.getId();

            Context context = new Context();
            context.setVariable("ticketNumber", ticket.getTicketNumber());
            context.setVariable("title", ticket.getTitle());
            context.setVariable("priority", ticket.getPriority().name());
            context.setVariable("clientName", ticket.getClient().getCompanyName());
            context.setVariable("assignerName", assignerName);
            context.setVariable("ticketUrl", ticketUrl);

            String htmlContent = templateEngine.process("ticket-assigned", context);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Ticket Assigned to You: [" + ticket.getTicketNumber() + "] " + ticket.getTitle());
            
            String plainText = String.format("Hello,\n\nA ticket has just been assigned to you by %s.\nTicket Number: %s\nTitle: %s\nPriority: %s\nClient: %s\n\nView Ticket: %s",
                    assignerName, ticket.getTicketNumber(), ticket.getTitle(), ticket.getPriority().name(), ticket.getClient().getCompanyName(), ticketUrl);
            
            helper.setText(plainText, htmlContent);

            javaMailSender.send(message);
            log.info("Ticket Assigned email sent successfully to: {}", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send assignment email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
