package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Notification;
import com.eventbooking.enums.NotificationType;
import com.eventbooking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${spring.mail.username:noreply@eventpass.com}")
    private String fromEmail;

    @Async("notificationTaskExecutor")
    public void sendBookingHoldNotice(Long userId, Long bookingId, String eventName, java.math.BigDecimal totalAmount, Instant holdExpiresAt, String recipientEmail) {
        String subject = "Seats Held — Complete Your Booking for " + eventName;
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 12px;">
                <h2 style="color: #6366f1;">🎟️ Seats Reserved (10-Minute Hold)</h2>
                <p>Hello,</p>
                <p>Your seats for <strong>%s</strong> have been placed on hold.</p>
                <p>Hold Total: <strong>$%.2f</strong></p>
                <p>Please complete your payment before your hold expires at <strong>%s</strong>.</p>
                <br/>
                <p style="color: #94a3b8; font-size: 0.85em;">Event Ticket Booking System</p>
            </div>
            """, eventName, totalAmount, holdExpiresAt);

        dispatchNotification(userId, bookingId, NotificationType.BOOKING_HOLD, recipientEmail, subject, htmlContent);
    }

    @Async("notificationTaskExecutor")
    public void sendBookingConfirmation(Long userId, Long bookingId, String eventName, String venueName, java.math.BigDecimal totalAmount, String recipientEmail) {
        String subject = "Booking Confirmed! — " + eventName;
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 12px;">
                <h2 style="color: #10b981;">🎉 Booking Confirmation</h2>
                <p>Congratulations! Your tickets for <strong>%s</strong> are confirmed.</p>
                <p>Booking ID: <strong>#%d</strong></p>
                <p>Amount Paid: <strong>$%.2f</strong></p>
                <p>Venue: %s</p>
                <br/>
                <p style="color: #94a3b8; font-size: 0.85em;">Enjoy the event!</p>
            </div>
            """, eventName, bookingId, totalAmount, venueName != null ? venueName : "Main Stadium");

        dispatchNotification(userId, bookingId, NotificationType.BOOKING_CONFIRMATION, recipientEmail, subject, htmlContent);
    }

    @Async("notificationTaskExecutor")
    public void sendCancellationNotice(Long userId, Long bookingId, String eventName, String recipientEmail, String reason) {
        String subject = "Booking Cancellation — " + eventName;
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 12px;">
                <h2 style="color: #ef4444;">Booking Cancelled</h2>
                <p>Booking #%d for <strong>%s</strong> has been cancelled.</p>
                <p>Reason: %s</p>
                <p>Released seats are now available for re-booking.</p>
            </div>
            """, bookingId, eventName, reason);

        dispatchNotification(userId, bookingId, NotificationType.BOOKING_CANCELLATION, recipientEmail, subject, htmlContent);
    }

    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    private void dispatchNotification(Long userId, Long bookingId, NotificationType type, String toEmail, String subject, String bodyHtml) {
        Notification notification = Notification.builder()
                .userId(userId)
                .bookingId(bookingId)
                .type(type)
                .status("SENDING")
                .message(subject)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Try raw SMTP first
        boolean sent = false;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            helper.setFrom(fromEmail, "EventPass Tickets");

            mailSender.send(mimeMessage);
            sent = true;
            saved.setStatus("SENT");
            saved.setSentAt(Instant.now());
            log.info("Async SMTP email sent successfully to {} for notification #{}", toEmail, saved.getId());
        } catch (Exception e) {
            log.warn("Raw SMTP port blocked by cloud host for notification #{}. Attempting HTTPS REST API fallback...", saved.getId());
            // Attempt HTTPS REST API fallback (Port 443, never blocked by cloud firewalls)
            if (tryResendHttpsFallback(toEmail, subject, bodyHtml)) {
                sent = true;
                saved.setStatus("SENT");
                saved.setSentAt(Instant.now());
            } else {
                saved.setStatus("FAILED");
                log.error("SMTP & HTTPS email delivery blocked by cloud firewall for notification #{}. Ticket is confirmed in database.", saved.getId());
            }
        } finally {
            notificationRepository.save(saved);
        }
    }

    private boolean tryResendHttpsFallback(String toEmail, String subject, String bodyHtml) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.info("No RESEND_API_KEY configured. To enable live email delivery on Render's firewall-restricted network, set RESEND_API_KEY environment variable.");
            return false;
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String jsonPayload = "{"
                    + "\"from\":\"EventPass Tickets <onboarding@resend.dev>\","
                    + "\"to\":[\"" + toEmail + "\"],"
                    + "\"subject\":\"" + subject.replace("\"", "\\\"") + "\","
                    + "\"html\":\"" + bodyHtml.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\""
                    + "}";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("HTTPS REST Email delivered successfully via Resend API to {}", toEmail);
                return true;
            } else {
                log.warn("Resend API response status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.error("HTTPS REST Email fallback failed: {}", ex.getMessage());
        }
        return false;
    }
}
