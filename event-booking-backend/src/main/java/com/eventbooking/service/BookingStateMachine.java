package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.enums.BookingStatus;
import com.eventbooking.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(BookingStatus.PENDING, EnumSet.of(BookingStatus.HELD, BookingStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(BookingStatus.HELD, EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.EXPIRED, BookingStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(BookingStatus.CONFIRMED, EnumSet.of(BookingStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
    }

    public void transition(Booking booking, BookingStatus targetStatus) {
        BookingStatus currentStatus = booking.getStatus();
        Set<BookingStatus> validTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(BookingStatus.class));

        if (!validTargets.contains(targetStatus)) {
            throw new IllegalStateTransitionException(currentStatus, targetStatus);
        }

        booking.setStatus(targetStatus);
    }
}
