package com.bookmyroute.service;

import com.bookmyroute.entity.Booking;
import com.bookmyroute.dto.response.EmailDeliveryResponse;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final BookingPdfService bookingPdfService;
    private final boolean mailEnabled;
    private final String mailUsername;
    private final String mailPassword;
    private final String fromEmail;
    private final String senderName;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        BookingPdfService bookingPdfService,
                        @Value("${app.mail.enabled:true}") boolean mailEnabled,
                        @Value("${spring.mail.username:}") String mailUsername,
                        @Value("${spring.mail.password:}") String mailPassword,
                        @Value("${app.mail.from}") String fromEmail,
                        @Value("${app.mail.sender-name:BookMyRoute}") String senderName) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.bookingPdfService = bookingPdfService;
        this.mailEnabled = mailEnabled;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.fromEmail = fromEmail;
        this.senderName = senderName;
    }

    public EmailDeliveryResponse sendBookingConfirmation(Booking booking) {
        String recipient = booking.getUser().getEmail();
        try {
            log.info("Sending booking confirmation email to {} for {}", recipient, booking.getBookingRef());
            EmailDeliveryResponse readiness = validateMailSettings(recipient);
            if (!readiness.isConfigured()) {
                logDelivery("booking confirmation", booking.getBookingRef(), readiness);
                return readiness;
            }
            Context context = createBookingContext(booking);
            String html = templateEngine.process("email/booking-confirmation", context);
            EmailAttachment ticketAttachment = createTicketAttachment(booking);
            EmailDeliveryResponse delivery = sendHtmlEmail(
                    recipient,
                    "Booking Confirmed - " + booking.getBookingRef(),
                    html,
                    ticketAttachment
            );
            logDelivery("booking confirmation", booking.getBookingRef(), delivery);
            return delivery;
        } catch (Exception ex) {
            log.warn("Booking confirmation email failed before send for {}: {}", booking.getBookingRef(), ex.getMessage(), ex);
            return EmailDeliveryResponse.failed(recipient, isConfigured(resolveFromAddress()),
                    "Email notification failed: " + ex.getMessage());
        }
    }

    public EmailDeliveryResponse sendBookingCancellation(Booking booking) {
        String recipient = booking.getUser().getEmail();
        try {
            log.info("Sending booking cancellation email to {} for {}", recipient, booking.getBookingRef());
            EmailDeliveryResponse readiness = validateMailSettings(recipient);
            if (!readiness.isConfigured()) {
                logDelivery("booking cancellation", booking.getBookingRef(), readiness);
                return readiness;
            }
            Context context = createBookingContext(booking);
            String html = templateEngine.process("email/booking-cancellation", context);
            EmailDeliveryResponse delivery = sendHtmlEmail(
                    recipient,
                    "Booking Cancelled - " + booking.getBookingRef(),
                    html
            );
            logDelivery("booking cancellation", booking.getBookingRef(), delivery);
            return delivery;
        } catch (Exception ex) {
            log.warn("Booking cancellation email failed before send for {}: {}", booking.getBookingRef(), ex.getMessage(), ex);
            return EmailDeliveryResponse.failed(recipient, isConfigured(resolveFromAddress()),
                    "Email notification failed: " + ex.getMessage());
        }
    }

    public EmailDeliveryResponse sendTestEmail(String to) {
        try {
            EmailDeliveryResponse readiness = validateMailSettings(to);
            if (!readiness.isConfigured()) {
                logDelivery("test email", "manual-test", readiness);
                return readiness;
            }
            String html = """
                    <html>
                        <body style="font-family:Arial, Helvetica, sans-serif; color:#1f2937;">
                            <h2>BookMyRoute email test</h2>
                            <p>Your SMTP configuration is working.</p>
                        </body>
                    </html>
                    """;
            EmailDeliveryResponse delivery = sendHtmlEmail(to, "BookMyRoute email test", html);
            logDelivery("test email", "manual-test", delivery);
            return delivery;
        } catch (Exception ex) {
            log.warn("Test email failed before send for {}: {}", to, ex.getMessage(), ex);
            return EmailDeliveryResponse.failed(to, isConfigured(resolveFromAddress()),
                    "Email notification failed: " + ex.getMessage());
        }
    }

    private Context createBookingContext(Booking booking) {
        Context context = new Context();
        context.setVariable("booking", booking);
        context.setVariable("user", booking.getUser());
        context.setVariable("schedule", booking.getSchedule());
        context.setVariable("route", booking.getSchedule().getRoute());
        context.setVariable("bus", booking.getSchedule().getBus());
        context.setVariable("seats", booking.getBookingSeats());
        return context;
    }

    private EmailDeliveryResponse sendHtmlEmail(String to, String subject, String html) {
        return sendHtmlEmail(to, subject, html, null);
    }

    private EmailDeliveryResponse sendHtmlEmail(String to, String subject, String html, EmailAttachment attachment) {
        try {
            String from = resolveFromAddress();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(from, senderName);
            helper.setText(html, true);
            if (attachment != null) {
                helper.addAttachment(
                        attachment.filename(),
                        new ByteArrayResource(attachment.content()),
                        attachment.contentType()
                );
            }

            mailSender.send(message);
            return EmailDeliveryResponse.sent(to);
        } catch (MailException | UnsupportedEncodingException | jakarta.mail.MessagingException ex) {
            log.warn("Email send failed for {}: {}", to, ex.getMessage(), ex);
            return EmailDeliveryResponse.failed(to, true, explainMailFailure(ex));
        }
    }

    private EmailDeliveryResponse validateMailSettings(String to) {
        String from = resolveFromAddress();
        if (!StringUtils.hasText(to)) {
            return EmailDeliveryResponse.failed(to, isConfigured(from), "Recipient email address is missing");
        }
        if (!mailEnabled) {
            return EmailDeliveryResponse.skipped(to, "Email notifications are disabled. Set MAIL_ENABLED=true to send emails.");
        }
        if (!StringUtils.hasText(mailUsername)) {
            return EmailDeliveryResponse.skipped(to, "MAIL_USERNAME is not configured");
        }
        if (!StringUtils.hasText(mailPassword)) {
            return EmailDeliveryResponse.skipped(to, "MAIL_PASSWORD is not configured. Use a Gmail App Password for Gmail SMTP.");
        }
        if (!StringUtils.hasText(from)) {
            return EmailDeliveryResponse.skipped(to, "Mail sender address is not configured. Set MAIL_FROM or MAIL_USERNAME.");
        }
        return new EmailDeliveryResponse(false, true, to, "Email is configured");
    }

    private String resolveFromAddress() {
        return StringUtils.hasText(fromEmail) ? fromEmail : mailUsername;
    }

    private EmailAttachment createTicketAttachment(Booking booking) {
        byte[] pdf = bookingPdfService.generateTicketPdf(booking);
        return new EmailAttachment(
                "BookMyRoute-" + booking.getBookingRef() + ".pdf",
                pdf,
                "application/pdf"
        );
    }

    private boolean isConfigured(String from) {
        return mailEnabled
                && StringUtils.hasText(mailUsername)
                && StringUtils.hasText(mailPassword)
                && StringUtils.hasText(from);
    }

    private String explainMailFailure(Exception ex) {
        String message = ex.getMessage();
        String lower = message == null ? "" : message.toLowerCase();
        if (lower.contains("authentication") || lower.contains("username and password not accepted")
                || lower.contains("535") || lower.contains("bad credentials")) {
            return "Gmail rejected the login. Set MAIL_PASSWORD to a Gmail App Password for "
                    + mailUsername + ", not the Gmail account password or admin password.";
        }
        return "Email send failed: " + (StringUtils.hasText(message) ? message : ex.getClass().getSimpleName());
    }

    private void logDelivery(String type, String reference, EmailDeliveryResponse delivery) {
        if (delivery.isSent()) {
            log.info("{} sent to {} for {}", type, delivery.getRecipient(), reference);
        } else {
            log.warn("{} not sent to {} for {}: {}", type, delivery.getRecipient(), reference, delivery.getMessage());
        }
    }

    private record EmailAttachment(String filename, byte[] content, String contentType) {}
}
