package com.eventbooking.service;

import com.eventbooking.dto.EventResponseDto;
import com.eventbooking.dto.SeatResponseDto;
import com.eventbooking.dto.VenueResponseDto;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.EventSeat;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.EventSeatRepository;
import com.eventbooking.repository.EventSpecification;
import com.eventbooking.repository.SeatRepository;
import com.eventbooking.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public EventResponseDto createEvent(Long organizerId, com.eventbooking.dto.CreateEventRequestDto request) {
        com.eventbooking.entity.Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.getVenueId()));

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venue(venue)
                .organizerId(organizerId)
                .eventDate(request.getEventDate())
                .basePrice(request.getBasePrice())
                .status(com.eventbooking.enums.EventStatus.SCHEDULED)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Populate venue seats as EventSeat instances
        List<com.eventbooking.entity.Seat> seats = seatRepository.findByVenueId(venue.getId());
        List<EventSeat> eventSeats = new java.util.ArrayList<>();

        for (com.eventbooking.entity.Seat seat : seats) {
            java.math.BigDecimal seatPrice = "VIP".equalsIgnoreCase(seat.getSection()) 
                    ? request.getBasePrice().multiply(new java.math.BigDecimal("1.50")) 
                    : request.getBasePrice();

            eventSeats.add(EventSeat.builder()
                    .event(savedEvent)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(seatPrice)
                    .version(0L)
                    .build());
        }

        eventSeatRepository.saveAll(eventSeats);
        return mapToEventDto(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<VenueResponseDto> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(v -> VenueResponseDto.builder()
                        .id(v.getId())
                        .name(v.getName())
                        .address(v.getAddress())
                        .totalCapacity(v.getTotalCapacity())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDto> getEvents(String city, String keyword, Pageable pageable) {
        Specification<Event> spec = Specification.where(EventSpecification.hasCity(city))
                .and(EventSpecification.searchKeyword(keyword));

        return eventRepository.findAll(spec, pageable).map(this::mapToEventDto);
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        return mapToEventDto(event);
    }

    @Transactional(readOnly = true)
    public List<SeatResponseDto> getEventSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with ID: " + eventId);
        }

        List<EventSeat> eventSeats = eventSeatRepository.findByEventIdWithSeatDetails(eventId);
        return eventSeats.stream()
                .map(es -> SeatResponseDto.builder()
                        .eventSeatId(es.getId())
                        .seatId(es.getSeat().getId())
                        .seatLabel(es.getSeat().getSeatLabel())
                        .section(es.getSeat().getSection())
                        .status(es.getStatus())
                        .price(es.getPrice())
                        .build())
                .toList();
    }

    private EventResponseDto mapToEventDto(Event event) {
        long availableCount = eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);
        long totalCount = eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE)
                + eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.HELD)
                + eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.BOOKED);

        VenueResponseDto venueDto = VenueResponseDto.builder()
                .id(event.getVenue().getId())
                .name(event.getVenue().getName())
                .address(event.getVenue().getAddress())
                .totalCapacity(event.getVenue().getTotalCapacity())
                .build();

        return EventResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .venue(venueDto)
                .organizerId(event.getOrganizerId())
                .eventDate(event.getEventDate())
                .basePrice(event.getBasePrice())
                .status(event.getStatus())
                .availableSeats(availableCount)
                .totalSeats(totalCount)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
