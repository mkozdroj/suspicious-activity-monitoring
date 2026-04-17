package com.grad.sam.dto.response;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponseDto {
    @NotNull(message = "Case ID is required")
    @Positive(message = "Case ID must be positive")
    private Integer caseId;

    @NotNull(message = "Alert ID is required")
    @Positive(message = "Alert ID must be positive")
    private Integer alertId;

    @NotNull(message = "Alert severity is required")
    private AlertSeverity alertSeverity;

    @NotNull(message = "Case status is required")
    private InvestigationState status;

    @NotNull(message = "Assigned officer is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Assigned officer must be in the format 'First Last' with proper capitalization")
    private String assignedOfficer;

    @NotNull(message = "Opened at is required")
    private LocalDateTime openedAt;

    @NotNull(message = "Closed at is required")
    private LocalDateTime closedAt;

    @NotNull(message = "Outcome is required")
    private InvestigationOutcome outcome;
}
