package com.eventbooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreateEventRequestDto {

    @NotBlank(message = "Event name is required")
    private String name;

    private String description;

    @NotNull(message = "Venue ID is required")
    private Long venueId;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private Instant eventDate;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", message = "Base price cannot be negative")
    private BigDecimal basePrice;
}
