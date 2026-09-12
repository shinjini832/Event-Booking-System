package com.eventbooking.dto;

import com.eventbooking.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {
    private Long id;
    private String name;
    private String description;
    private VenueResponseDto venue;
    private Long organizerId;
    private Instant eventDate;
    private BigDecimal basePrice;
    private EventStatus status;
    private long availableSeats;
    private long totalSeats;
    private Instant createdAt;
}
