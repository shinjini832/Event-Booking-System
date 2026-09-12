package com.eventbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmBookingRequestDto {

    @NotBlank(message = "Payment token is required")
    private String paymentToken;
}
