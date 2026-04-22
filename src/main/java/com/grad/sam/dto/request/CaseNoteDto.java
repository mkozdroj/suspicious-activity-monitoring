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
public class CaseNoteDto {
    @NotBlank(message = "Author is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Author must be in the format 'First Last' with proper capitalization")
    private String author;

    @NotBlank(message = "Note text is required")
    @Size(max = 400, message = "Note text must be at most 400 characters")
    private String noteText;
}
