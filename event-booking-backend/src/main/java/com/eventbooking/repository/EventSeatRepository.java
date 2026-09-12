package com.eventbooking.repository;

import com.eventbooking.entity.EventSeat;
import com.eventbooking.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    List<EventSeat> findByEventId(Long eventId);

    List<EventSeat> findByEventIdAndIdIn(Long eventId, List<Long> ids);

    @Query("SELECT es FROM EventSeat es JOIN FETCH es.seat WHERE es.event.id = :eventId")
    List<EventSeat> findByEventIdWithSeatDetails(@Param("eventId") Long eventId);

    long countByEventIdAndStatus(Long eventId, SeatStatus status);
}
