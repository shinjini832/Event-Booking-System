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

    @Value("${spring.mail.username:shinjini832@gmail.com}")
    private String fromEmail;

    @Async("notificationTaskExecutor")
    public void sendBookingHoldNotice(Booking booking, String recipientEmail) {
        String subject = "Seats Held — Complete Your Booking for " + booking.getEvent().getName();
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
            """, booking.getEvent().getName(), booking.getTotalAmount(), booking.getHoldExpiresAt());

        dispatchNotification(booking.getUser().getId(), booking.getId(), NotificationType.BOOKING_HOLD, recipientEmail, subject, htmlContent);
    }

    @Async("notificationTaskExecutor")
    public void sendBookingConfirmation(Booking booking, String recipientEmail) {
        String subject = "Booking Confirmed! — " + booking.getEvent().getName();
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
            """, booking.getEvent().getName(), booking.getId(), booking.getTotalAmount(), booking.getEvent().getVenue().getName());

        dispatchNotification(booking.getUser().getId(), booking.getId(), NotificationType.BOOKING_CONFIRMATION, recipientEmail, subject, htmlContent);
    }

    @Async("notificationTaskExecutor")
    public void sendCancellationNotice(Booking booking, String recipientEmail, String reason) {
        String subject = "Booking Cancellation — " + booking.getEvent().getName();
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 12px;">
                <h2 style="color: #ef4444;">Booking Cancelled</h2>
                <p>Booking #%d for <strong>%s</strong> has been cancelled.</p>
                <p>Reason: %s</p>
                <p>Released seats are now available for re-booking.</p>
            </div>
            """, booking.getId(), booking.getEvent().getName(), reason);

        dispatchNotification(booking.getUser().getId(), booking.getId(), NotificationType.BOOKING_CANCELLATION, recipientEmail, subject, htmlContent);
    }

    private void dispatchNotification(Long userId, Long bookingId, NotificationType type, String toEmail, String subject, String bodyHtml) {
        Notification notification = Notification.builder()
                .userId(userId)
                .bookingId(bookingId)
                .type(type)
                .status("SENDING")
                .message(subject)
                .build();

        Notification saved = notificationRepository.save(notification);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            helper.setFrom(fromEmail, "EventPass Tickets");

            mailSender.send(mimeMessage);
            saved.setStatus("SENT");
            saved.setSentAt(Instant.now());
            log.info("Async SMTP email sent successfully to {} for notification #{}", toEmail, saved.getId());
        } catch (Exception e) {
            saved.setStatus("FAILED");
            log.error("SMTP delivery failed for notification #{}. Fallback logged locally: {}", saved.getId(), e.getMessage());
        } finally {
            notificationRepository.save(saved);
        }
    }
}
