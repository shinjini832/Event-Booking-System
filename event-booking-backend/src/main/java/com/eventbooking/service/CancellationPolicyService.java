package com.eventbooking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class CancellationPolicyService {

    public BigDecimal calculateRefundAmount(BigDecimal totalPaid, Instant eventDate, Instant cancellationTime) {
        if (eventDate == null || cancellationTime == null || cancellationTime.isAfter(eventDate)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long hoursRemaining = Duration.between(cancellationTime, eventDate).toHours();

        if (hoursRemaining >= 48) {
            // 100% refund
            return totalPaid.setScale(2, RoundingMode.HALF_UP);
        } else if (hoursRemaining >= 24) {
            // 50% refund
            return totalPaid.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        } else {
            // 0% refund
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
