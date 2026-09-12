package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import com.eventbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, Instant now);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.bookingSeats bs LEFT JOIN FETCH bs.eventSeat WHERE b.id = :id")
    Optional<Booking> findByIdWithSeats(@Param("id") Long id);
}
