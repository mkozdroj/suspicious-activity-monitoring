package com.grad.sam.dto.request;

import com.grad.sam.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenCaseRequestDto {

    @NotNull(message = "Alert ID is required")
    @Positive(message = "Alert ID must be positive")
    private Integer alertId;

    @NotBlank(message = "Assigned officer is required")
    @Pattern(
            regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$",
            message = "Assigned officer must be in the format 'First Last' with proper capitalization"
    )
    private String assignedOfficer;

    private Priority priority = Priority.MEDIUM;
}
