package com.eventbooking.dto;

import com.eventbooking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private Long userId;
    private Long eventId;
    private String eventName;
    private BookingStatus status;
    private Instant holdExpiresAt;
    private String idempotencyKey;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private List<BookingSeatDto> seats;
}
