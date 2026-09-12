package com.eventbooking.repository;

import com.eventbooking.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByEventIdAndStatusOrderByCreatedAtAsc(Long eventId, String status);
    Optional<Waitlist> findByEventIdAndUserIdAndStatus(Long eventId, Long userId, String status);
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, String status);
}
