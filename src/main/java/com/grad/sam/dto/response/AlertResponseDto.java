package com.grad.sam.dto.response;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDto {
    @NotBlank(message = "Alert ID is required")
    @Positive(message = "Alert ID must be positive")
    private Integer alertId;

    @NotBlank(message = "Transaction ID is required")
    @Positive(message = "Transaction ID must be positive")
    private Integer txnId;

    @NotBlank(message = "Transaction amount is required")
    @Positive(message = "Transaction amount must be positive")
    private BigDecimal txnAmount;

    @NotBlank(message = "Transaction currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Transaction currency must be a valid 3-letter ISO code")
    private String txnCurrency;

    @NotBlank(message = "Transaction date is required")
    private LocalDateTime txnDate;

    @NotBlank(message = "Account ID is required")
    @Positive(message = "Account ID must be positive")
    private Integer accountId;

    @NotBlank(message = "Customer name is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Customer name must be in the format 'First Last' with proper capitalization")
    private String customerName;

    @NotBlank(message = "Rule ID is required")
    @Positive(message = "Rule ID must be positive")
    private Integer ruleId;

    @NotBlank(message = "Rule category is required")
    private RuleCategory ruleCategory;

    @NotBlank(message = "Alert severity is required")
    private AlertSeverity severity;

    @NotBlank(message = "Alert status is required")
    private AlertStatus status;

    @NotBlank(message = "Raised at timestamp is required")
    private LocalDateTime raisedAt;

    @NotBlank(message = "Assigned to is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Assigned to must be in the format 'First Last' with proper capitalization")
    private String assignedTo;
}
