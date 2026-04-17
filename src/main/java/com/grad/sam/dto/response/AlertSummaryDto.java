package com.grad.sam.dto.response;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDto {
    @NotBlank(message = "Alert ID is required")
    @Positive(message = "Alert ID must be positive")
    private Integer alertId;

    @NotBlank(message = "Rule category is required")
    private RuleCategory ruleCategory;

    @NotBlank(message = "Severity is required")
    private AlertSeverity severity;

    @NotBlank(message = "Status is required")
    private AlertStatus status;

    @NotBlank(message = "Raised at is required")
    private LocalDateTime raisedAt;
}
