package com.grad.sam.model;

import com.grad.sam.enums.AlertSeverity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "alert_rule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "rule_code", nullable = false, unique = true, length = 20)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    // STRUCTURING, SMURFING, VELOCITY, WATCHLIST, GEOGRAPHY, PATTERN
    @Column(name = "rule_category", nullable = false, length = 30)
    private String ruleCategory;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "threshold_amount", precision = 18, scale = 2)
    private BigDecimal thresholdAmount;         // USD threshold (nullable)

    @Column(name = "threshold_count")
    private Integer thresholdCount;             // transaction count threshold (nullable)

    @Column(name = "lookback_days")
    private Integer lookbackDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private AlertSeverity severity;             // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;
}
