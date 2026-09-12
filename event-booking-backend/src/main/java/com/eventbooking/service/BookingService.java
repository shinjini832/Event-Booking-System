package com.eventbooking.service;

import com.eventbooking.dto.BookingResponseDto;
import com.eventbooking.dto.BookingSeatDto;
import com.eventbooking.dto.HoldRequestDto;
import com.eventbooking.entity.*;
import com.eventbooking.enums.BookingStatus;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.exception.BadRequestException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.SeatConflictException;
import com.eventbooking.repository.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final UserRepository userRepository;
    private final BookingStateMachine stateMachine;
    private final NotificationService notificationService;

    @Transactional
    public BookingResponseDto holdSeats(Long userId, HoldRequestDto request, String idempotencyKey) {
        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existingBooking = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existingBooking.isPresent()) {
                log.info("Idempotency key hit for key: {}", idempotencyKey);
                return mapToBookingResponse(existingBooking.get());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        List<EventSeat> eventSeats = eventSeatRepository.findByEventIdAndIdIn(request.getEventId(), request.getSeatIds());

        if (eventSeats.size() != request.getSeatIds().size()) {
            throw new BadRequestException("One or more requested seat IDs are invalid for event: " + request.getEventId());
        }

        // Check availability
        List<String> unavailableLabels = new ArrayList<>();
        for (EventSeat es : eventSeats) {
            if (es.getStatus() != SeatStatus.AVAILABLE) {
                unavailableLabels.add(es.getSeat().getSeatLabel());
            }
        }

        if (!unavailableLabels.isEmpty()) {
            throw new SeatConflictException("Seats no longer available: " + String.join(", ", unavailableLabels));
        }

        // Calculate price and hold seats with Optimistic Locking
        BigDecimal totalAmount = BigDecimal.ZERO;
        Instant holdExpiration = Instant.now().plus(10, ChronoUnit.MINUTES);

        try {
            for (EventSeat es : eventSeats) {
                es.setStatus(SeatStatus.HELD);
                totalAmount = totalAmount.add(es.getPrice());
                eventSeatRepository.save(es);
            }
            eventSeatRepository.flush(); // Force optimistic locking version check against DB
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock collision detected during seat hold for event {}", request.getEventId());
            throw new SeatConflictException("One or more seats were just taken by another user. Please choose different seats.");
        }

        // Save Booking
        Booking booking = Booking.builder()
                .user(user)
                .event(event)
                .status(BookingStatus.PENDING)
                .holdExpiresAt(holdExpiration)
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null)
                .totalAmount(totalAmount)
                .build();

        stateMachine.transition(booking, BookingStatus.HELD);
        Booking savedBooking = bookingRepository.save(booking);

        // Link BookingSeat items
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (EventSeat es : eventSeats) {
            BookingSeat bs = BookingSeat.builder()
                    .booking(savedBooking)
                    .eventSeat(es)
                    .build();
            bookingSeats.add(bs);
        }
        bookingSeatRepository.saveAll(bookingSeats);
        savedBooking.setBookingSeats(bookingSeats);

        // Trigger async hold notification
        notificationService.sendBookingHoldNotice(user.getId(), savedBooking.getId(), event.getName(), totalAmount, holdExpiration, user.getEmail());

        return mapToBookingResponse(savedBooking);
    }

    @Transactional
    public BookingResponseDto confirmBooking(Long userId, Long bookingId, String paymentToken) {
        Booking booking = bookingRepository.findByIdWithSeats(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to confirm this booking.");
        }

        if (booking.getStatus() != BookingStatus.HELD) {
            throw new BadRequestException("Booking cannot be confirmed as it is in state: " + booking.getStatus());
        }

        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Hold time expired for booking #" + bookingId + ". Please select seats again.");
        }

        // Simulate payment check
        if ("FAIL_PAYMENT".equalsIgnoreCase(paymentToken)) {
            throw new BadRequestException("Simulated payment authorization failed. You may retry payment within the hold window.");
        }

        // Transition booking status HELD -> CONFIRMED
        stateMachine.transition(booking, BookingStatus.CONFIRMED);

        // Mark seats permanently as BOOKED
        for (BookingSeat bs : booking.getBookingSeats()) {
            EventSeat es = bs.getEventSeat();
            es.setStatus(SeatStatus.BOOKED);
            eventSeatRepository.save(es);
        }

        Booking confirmedBooking = bookingRepository.save(booking);

        // Trigger async confirmation notification
        notificationService.sendBookingConfirmation(booking.getUser().getId(), confirmedBooking.getId(), booking.getEvent().getName(), booking.getEvent().getVenue().getName(), confirmedBooking.getTotalAmount(), booking.getUser().getEmail());

        return mapToBookingResponse(confirmedBooking);
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long userId, Long bookingId, String reason) {
        Booking booking = bookingRepository.findByIdWithSeats(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to cancel this booking.");
        }

        stateMachine.transition(booking, BookingStatus.CANCELLED);

        // Release seats back to AVAILABLE
        for (BookingSeat bs : booking.getBookingSeats()) {
            EventSeat es = bs.getEventSeat();
            es.setStatus(SeatStatus.AVAILABLE);
            eventSeatRepository.save(es);
        }

        Booking cancelledBooking = bookingRepository.save(booking);

        // Async cancellation notice
        notificationService.sendCancellationNotice(booking.getUser().getId(), cancelledBooking.getId(), booking.getEvent().getName(), booking.getUser().getEmail(), reason != null ? reason : "User requested cancellation");

        return mapToBookingResponse(cancelledBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdWithSeats(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to view this booking.");
        }

        return mapToBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    private BookingResponseDto mapToBookingResponse(Booking booking) {
        List<BookingSeatDto> seatDtos = booking.getBookingSeats().stream()
                .map(bs -> BookingSeatDto.builder()
                        .eventSeatId(bs.getEventSeat().getId())
                        .seatId(bs.getEventSeat().getSeat().getId())
                        .seatLabel(bs.getEventSeat().getSeat().getSeatLabel())
                        .section(bs.getEventSeat().getSeat().getSection())
                        .price(bs.getEventSeat().getPrice())
                        .build())
                .toList();

        return BookingResponseDto.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .eventId(booking.getEvent().getId())
                .eventName(booking.getEvent().getName())
                .status(booking.getStatus())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .idempotencyKey(booking.getIdempotencyKey())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .seats(seatDtos)
                .build();
    }
}
