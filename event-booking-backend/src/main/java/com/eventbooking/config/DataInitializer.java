package com.eventbooking.config;

import com.eventbooking.entity.*;
import com.eventbooking.enums.EventStatus;
import com.eventbooking.enums.Role;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data population.");
            return;
        }

        log.info("Seeding initial demo data for Event Booking Platform...");

        // 1. Users
        User admin = userRepository.save(User.builder()
                .email("admin@event.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        User organizer = userRepository.save(User.builder()
                .email("organizer@event.com")
                .password(passwordEncoder.encode("organizer123"))
                .role(Role.ORGANIZER)
                .build());

        User demoUser = userRepository.save(User.builder()
                .email("user@event.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        // 2. Venue
        Venue venue = venueRepository.save(Venue.builder()
                .name("Grand Symphony Hall")
                .address("777 Metropolitan Blvd, New York, NY")
                .totalCapacity(50)
                .build());

        // 3. Seats (Section VIP: A1-A10, Section Floor: B1-B15, Section Balcony: C1-C15)
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            seats.add(Seat.builder().venue(venue).section("VIP").seatLabel("VIP-" + i).build());
        }
        for (int i = 1; i <= 15; i++) {
            seats.add(Seat.builder().venue(venue).section("Main Floor").seatLabel("FL-" + i).build());
        }
        for (int i = 1; i <= 15; i++) {
            seats.add(Seat.builder().venue(venue).section("Balcony").seatLabel("BAL-" + i).build());
        }
        List<Seat> savedSeats = seatRepository.saveAll(seats);

        // 4. Events
        Event event1 = eventRepository.save(Event.builder()
                .name("Coldplay: Music of the Spheres World Tour")
                .description("Experience an incredible night of immersive stadium rock, lights, and world-class live performance.")
                .venue(venue)
                .organizerId(organizer.getId())
                .eventDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .basePrice(new BigDecimal("120.00"))
                .status(EventStatus.SCHEDULED)
                .build());

        Event event2 = eventRepository.save(Event.builder()
                .name("Global AI & Tech Innovation Summit 2026")
                .description("Keynotes from industry leaders on AI agents, neural architectures, and software engineering futures.")
                .venue(venue)
                .organizerId(organizer.getId())
                .eventDate(Instant.now().plus(14, ChronoUnit.DAYS))
                .basePrice(new BigDecimal("250.00"))
                .status(EventStatus.SCHEDULED)
                .build());

        // 5. EventSeats
        List<EventSeat> eventSeats1 = new ArrayList<>();
        for (Seat seat : savedSeats) {
            BigDecimal seatPrice = "VIP".equals(seat.getSection()) ? new BigDecimal("180.00") : new BigDecimal("120.00");
            eventSeats1.add(EventSeat.builder()
                    .event(event1)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(seatPrice)
                    .version(0L)
                    .build());
        }
        eventSeatRepository.saveAll(eventSeats1);

        List<EventSeat> eventSeats2 = new ArrayList<>();
        for (Seat seat : savedSeats) {
            BigDecimal seatPrice = "VIP".equals(seat.getSection()) ? new BigDecimal("350.00") : new BigDecimal("250.00");
            eventSeats2.add(EventSeat.builder()
                    .event(event2)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(seatPrice)
                    .version(0L)
                    .build());
        }
        eventSeatRepository.saveAll(eventSeats2);

        log.info("Demo data seeding completed successfully! Created 2 Events and 80 EventSeats.");
    }
}
