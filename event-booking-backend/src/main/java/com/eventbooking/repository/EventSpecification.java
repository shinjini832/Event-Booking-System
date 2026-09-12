package com.eventbooking.repository;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.Venue;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class EventSpecification {

    public static Specification<Event> hasCity(String city) {
        return (root, query, cb) -> {
            if (city == null || city.isBlank()) return null;
            Join<Event, Venue> venueJoin = root.join("venue");
            return cb.like(cb.lower(venueJoin.get("address")), "%" + city.toLowerCase() + "%");
        };
    }

    public static Specification<Event> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Event> upcomingEventsOnly() {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), Instant.now());
    }
}
