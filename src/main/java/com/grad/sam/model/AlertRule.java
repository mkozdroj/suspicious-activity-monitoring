package com.grad.sam.model;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotBlank
    @Size(max = 20)
    @Column(name = "rule_code", nullable = false, unique = true, length = 20)
    private String ruleCode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_category", nullable = false, length = 30)
    private RuleCategory ruleCategory;

    @NotBlank
    @Size(max = 255)
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Positive
    @Digits(integer = 16, fraction = 2)
    @Column(name = "threshold_amount", precision = 18, scale = 2)
    private BigDecimal thresholdAmount;         // USD threshold (nullable)

    @Positive
    @Column(name = "threshold_count")
    private Integer thresholdCount;            // transaction count threshold (nullable)

    @Min(1)
    @Column(name = "lookback_days")
    private Integer lookbackDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private AlertSeverity severity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;
}
