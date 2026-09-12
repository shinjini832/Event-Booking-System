package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.enums.BookingStatus;
import com.eventbooking.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingStateMachineTest {

    private BookingStateMachine stateMachine;
    private Booking booking;

    @BeforeEach
    void setUp() {
        stateMachine = new BookingStateMachine();
        booking = Booking.builder().status(BookingStatus.PENDING).build();
    }

    @Test
    void testLegalTransitionsFromPending() {
        stateMachine.transition(booking, BookingStatus.HELD);
        assertEquals(BookingStatus.HELD, booking.getStatus());
    }

    @Test
    void testLegalTransitionsFromHeldToConfirmed() {
        booking.setStatus(BookingStatus.HELD);
        stateMachine.transition(booking, BookingStatus.CONFIRMED);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void testLegalTransitionsFromHeldToExpired() {
        booking.setStatus(BookingStatus.HELD);
        stateMachine.transition(booking, BookingStatus.EXPIRED);
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    @Test
    void testLegalTransitionsFromHeldToCancelled() {
        booking.setStatus(BookingStatus.HELD);
        stateMachine.transition(booking, BookingStatus.CANCELLED);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void testLegalTransitionsFromConfirmedToCancelled() {
        booking.setStatus(BookingStatus.CONFIRMED);
        stateMachine.transition(booking, BookingStatus.CANCELLED);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void testIllegalTransitionFromPendingToConfirmed() {
        assertThrows(IllegalStateTransitionException.class, () -> 
            stateMachine.transition(booking, BookingStatus.CONFIRMED)
        );
    }

    @Test
    void testIllegalTransitionFromExpiredToConfirmed() {
        booking.setStatus(BookingStatus.EXPIRED);
        assertThrows(IllegalStateTransitionException.class, () -> 
            stateMachine.transition(booking, BookingStatus.CONFIRMED)
        );
    }

    @Test
    void testIllegalTransitionFromCancelledToHeld() {
        booking.setStatus(BookingStatus.CANCELLED);
        assertThrows(IllegalStateTransitionException.class, () -> 
            stateMachine.transition(booking, BookingStatus.HELD)
        );
    }
}
