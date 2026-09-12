package com.eventbooking.exception;

import com.eventbooking.enums.BookingStatus;

public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(BookingStatus current, BookingStatus target) {
        super(String.format("Illegal state transition from %s to %s", current, target));
    }
}
