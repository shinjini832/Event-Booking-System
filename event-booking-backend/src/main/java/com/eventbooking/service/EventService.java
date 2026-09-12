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
