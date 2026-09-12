package com.eventbooking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationPolicyTest {

    private CancellationPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new CancellationPolicyService();
    }

    @ParameterizedTest
    @CsvSource({
        "72, 100.00, 100.00",  // > 48h -> 100% refund
        "50, 100.00, 100.00",  // > 48h boundary -> 100% refund
        "36, 100.00, 50.00",   // 24-48h -> 50% refund
        "24, 100.00, 50.00",   // 24h boundary -> 50% refund
        "12, 100.00, 0.00",    // < 24h -> 0% refund
        "0,  100.00, 0.00"     // At event time -> 0% refund
    })
    void testRefundCalculationAcrossTimeBoundaries(long hoursBeforeEvent, String totalPaidStr, String expectedRefundStr) {
        Instant eventTime = Instant.now().plus(hoursBeforeEvent, ChronoUnit.HOURS);
        Instant cancelTime = Instant.now();

        BigDecimal totalPaid = new BigDecimal(totalPaidStr);
        BigDecimal expectedRefund = new BigDecimal(expectedRefundStr);

        BigDecimal actualRefund = policyService.calculateRefundAmount(totalPaid, eventTime, cancelTime);
        assertEquals(expectedRefund, actualRefund);
    }
}
