package com.eventbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequestDto {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
