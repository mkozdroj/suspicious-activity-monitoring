package com.grad.sam.dto.response;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDto {
    @NotNull(message = "Alert ID is required")
    @Positive(message = "Alert ID must be positive")
    private Integer alertId;

    @NotNull(message = "Rule category is required")
    private RuleCategory ruleCategory;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotNull(message = "Status is required")
    private AlertStatus status;

    @NotNull(message = "Raised at is required")
    private LocalDateTime raisedAt;
}
