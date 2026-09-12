package com.eventbooking.service;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.PricingRule;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.repository.EventSeatRepository;
import com.eventbooking.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final EventSeatRepository eventSeatRepository;

    public BigDecimal calculateSeatPrice(Event event) {
        BigDecimal basePrice = event.getBasePrice();

        long bookedCount = eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.BOOKED);
        long heldCount = eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.HELD);
        long availableCount = eventSeatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);

        long totalSeats = bookedCount + heldCount + availableCount;
        if (totalSeats == 0) {
            return basePrice;
        }

        double percentSoldOrHeld = ((double) (bookedCount + heldCount) / totalSeats) * 100.0;
        List<PricingRule> rules = pricingRuleRepository.findByEventId(event.getId());

        BigDecimal activeMultiplier = BigDecimal.ONE;

        for (PricingRule rule : rules) {
            if ("EARLY_BIRD".equalsIgnoreCase(rule.getRuleType()) && percentSoldOrHeld <= rule.getThresholdPercent()) {
                activeMultiplier = activeMultiplier.multiply(rule.getMultiplier());
            } else if ("SURGE".equalsIgnoreCase(rule.getRuleType()) && percentSoldOrHeld >= rule.getThresholdPercent()) {
                activeMultiplier = activeMultiplier.multiply(rule.getMultiplier());
            }
        }

        return basePrice.multiply(activeMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
