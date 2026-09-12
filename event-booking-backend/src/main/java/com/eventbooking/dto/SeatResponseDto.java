package com.eventbooking.dto;

import com.eventbooking.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponseDto {
    private Long eventSeatId;
    private Long seatId;
    private String seatLabel;
    private String section;
    private SeatStatus status;
    private BigDecimal price;
}
