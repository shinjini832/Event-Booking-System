package com.eventbooking.service;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.PricingRule;
import com.eventbooking.enums.SeatStatus;
import com.eventbooking.repository.EventSeatRepository;
import com.eventbooking.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class PricingServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @InjectMocks
    private PricingService pricingService;

    private Event event;

    @BeforeEach
    void setUp() {
        openMocks(this);
        event = Event.builder()
                .id(1L)
                .basePrice(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void testEarlyBirdDiscountAppliesWhenCapacityLow() {
        when(eventSeatRepository.countByEventIdAndStatus(eq(1L), eq(SeatStatus.BOOKED))).thenReturn(0L);
        when(eventSeatRepository.countByEventIdAndStatus(eq(1L), eq(SeatStatus.HELD))).thenReturn(1L);
        when(eventSeatRepository.countByEventIdAndStatus(eq(1L), eq(SeatStatus.AVAILABLE))).thenReturn(9L);

        PricingRule earlyBirdRule = PricingRule.builder()
                .ruleType("EARLY_BIRD")
                .thresholdPercent(20)
                .multiplier(new BigDecimal("0.80"))
                .build();

        when(pricingRuleRepository.findByEventId(1L)).thenReturn(List.of(earlyBirdRule));

        BigDecimal calculatedPrice = pricingService.calculateSeatPrice(event);
        assertEquals(new BigDecimal("80.00"), calculatedPrice);
    }

    @Test
    void testSurgePricingAppliesWhenCapacityHigh() {
        when(eventSeatRepository.countByEventIdAndStatus(eq(1L), eq(SeatStatus.BOOKED))).thenReturn(8L);
        when(eventSeatRepository.countByEventIdAndStatus(eq(1L), eq(SeatStatus.HELD))).thenReturn(0L);
        when(eventSeatRepository.countByEventIdAndStatus(eq(1L), eq(SeatStatus.AVAILABLE))).thenReturn(2L);

        PricingRule surgeRule = PricingRule.builder()
                .ruleType("SURGE")
                .thresholdPercent(75)
                .multiplier(new BigDecimal("1.50"))
                .build();

        when(pricingRuleRepository.findByEventId(1L)).thenReturn(List.of(surgeRule));

        BigDecimal calculatedPrice = pricingService.calculateSeatPrice(event);
        assertEquals(new BigDecimal("150.00"), calculatedPrice);
    }
}
