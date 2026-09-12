package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.BookingSeat;
import com.eventbooking.entity.EventSeat;
import com.eventbooking.enums.BookingStatus;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;
    private final BookingStateMachine stateMachine;

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void sweepExpiredHolds() {
        Instant now = Instant.now();
        List<Booking> expiredHolds = bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.HELD, now);

        if (expiredHolds.isEmpty()) {
            return;
        }

        log.info("Found {} expired seat holds. Starting cleanup sweep...", expiredHolds.size());

        for (Booking booking : expiredHolds) {
            try {
                processSingleHoldExpiry(booking.getId());
            } catch (Exception e) {
                log.error("Failed to expire hold for booking #{}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleHoldExpiry(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithSeats(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.HELD) {
            return;
        }

        stateMachine.transition(booking, BookingStatus.EXPIRED);

        for (BookingSeat bs : booking.getBookingSeats()) {
            EventSeat es = bs.getEventSeat();
            if (es.getStatus() == SeatStatus.HELD) {
                es.setStatus(SeatStatus.AVAILABLE);
                eventSeatRepository.save(es);
            }
        }

        bookingRepository.save(booking);
        log.info("Hold expired and seats released for booking #{}", bookingId);
    }
}
