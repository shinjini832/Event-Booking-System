package com.eventbooking.controller;

import com.eventbooking.dto.BookingResponseDto;
import com.eventbooking.dto.ConfirmBookingRequestDto;
import com.eventbooking.dto.HoldRequestDto;
import com.eventbooking.security.UserPrincipal;
import com.eventbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    public ResponseEntity<BookingResponseDto> holdSeats(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody HoldRequestDto request
    ) {
        BookingResponseDto response = bookingService.holdSeats(userPrincipal.getId(), request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDto> confirmBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ConfirmBookingRequestDto request
    ) {
        BookingResponseDto response = bookingService.confirmBooking(userPrincipal.getId(), id, request.getPaymentToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        BookingResponseDto response = bookingService.cancelBooking(userPrincipal.getId(), id, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingService.getBookingById(userPrincipal.getId(), id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDto>> getUserBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(bookingService.getUserBookings(userPrincipal.getId()));
    }
}
