package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // EARLY_BIRD, SURGE, FLAT

    @Column(name = "threshold_percent", nullable = false)
    private Integer thresholdPercent;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
