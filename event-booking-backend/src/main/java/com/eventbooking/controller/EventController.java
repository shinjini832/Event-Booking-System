package com.eventbooking.controller;

import com.eventbooking.dto.EventResponseDto;
import com.eventbooking.dto.SeatResponseDto;
import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventResponseDto>> getEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.getEvents(city, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatResponseDto>> getEventSeats(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventSeats(id));
    }
}
