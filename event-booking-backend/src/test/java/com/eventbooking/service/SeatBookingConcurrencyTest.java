package com.eventbooking.service;

import com.eventbooking.dto.HoldRequestDto;
import com.eventbooking.entity.*;
import com.eventbooking.enums.EventStatus;
import com.eventbooking.enums.Role;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.exception.SeatConflictException;
import com.eventbooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class SeatBookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    private Event testEvent;
    private EventSeat testEventSeat;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        eventSeatRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create venue
        Venue venue = venueRepository.save(Venue.builder()
                .name("Grand Arena")
                .address("100 Main St")
                .totalCapacity(1)
                .build());

        // 2. Create seat
        Seat seat = seatRepository.save(Seat.builder()
                .venue(venue)
                .section("VIP")
                .seatLabel("A1")
                .build());

        // 3. Create organizer & event
        User organizer = userRepository.save(User.builder()
                .email("organizer@event.com")
                .password("password123")
                .role(Role.ORGANIZER)
                .build());

        testEvent = eventRepository.save(Event.builder()
                .name("Concurrency Championship Concert")
                .description("High concurrency test event")
                .venue(venue)
                .organizerId(organizer.getId())
                .eventDate(Instant.now().plusSeconds(86400))
                .basePrice(new BigDecimal("150.00"))
                .status(EventStatus.SCHEDULED)
                .build());

        // 4. Create single AVAILABLE EventSeat
        testEventSeat = eventSeatRepository.save(EventSeat.builder()
                .event(testEvent)
                .seat(seat)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("150.00"))
                .version(0L)
                .build());

        // 5. Create 10 concurrent users
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User user = userRepository.save(User.builder()
                    .email("user" + i + "@concurrency.com")
                    .password("pass123")
                    .role(Role.USER)
                    .build());
            testUsers.add(user);
        }
    }

    @Test
    @DisplayName("Centerpiece Concurrency Test: 10 threads attempt simultaneous hold on 1 seat -> Exactly 1 succeeds, 9 fail")
    void testConcurrentHoldOnSingleSeat() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final User user = testUsers.get(i);
            final String idempotencyKey = "key-thread-" + i;

            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for release signal so all 10 threads fire at the exact same instant

                    HoldRequestDto holdRequest = new HoldRequestDto();
                    holdRequest.setEventId(testEvent.getId());
                    holdRequest.setSeatIds(List.of(testEventSeat.getId()));

                    bookingService.holdSeats(user.getId(), holdRequest, idempotencyKey);
                    successCount.incrementAndGet();
                } catch (SeatConflictException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    conflictCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all 10 threads simultaneously!
        finishLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // ASSERTIONS
        assertEquals(1, successCount.get(), "Exactly ONE thread must succeed in holding the seat");
        assertEquals(9, conflictCount.get(), "Exactly NINE threads must receive a seat conflict exception");

        // Verify DB State
        EventSeat reloadedSeat = eventSeatRepository.findById(testEventSeat.getId()).orElseThrow();
        assertEquals(SeatStatus.HELD, reloadedSeat.getStatus(), "Seat status must be HELD in DB");

        long bookingCount = bookingRepository.count();
        assertEquals(1, bookingCount, "Exactly ONE Booking row must be persisted in DB");
    }
}
