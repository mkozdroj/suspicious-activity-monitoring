package com.grad.sam.dto.response;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import jakarta.validation.constraints.NotNull;
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
    private String assignedOfficer;

    @NotNull(message = "Opened at is required")
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private InvestigationOutcome outcome;

    private String findings;
}
