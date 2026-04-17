package com.grad.sam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenCaseDto {
    @NotBlank(message = "Case ID is required")
    private Integer caseId;

    @NotBlank(message = "Author is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Author must be in the format 'First Last' with proper capitalization")
    private String author;

    @NotBlank(message = "Note text is required")
    @Size(min = 20, max = 500, message = "Note text must be between 20 and 500 characters")
    private String noteText;
}
