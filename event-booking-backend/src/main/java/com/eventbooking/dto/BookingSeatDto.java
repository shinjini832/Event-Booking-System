package com.eventbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatDto {
    private Long eventSeatId;
    private Long seatId;
    private String seatLabel;
    private String section;
    private BigDecimal price;
}
