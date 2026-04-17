package com.grad.sam.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistSearchDto {
    @NotBlank(message = "Name is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Name must be in the format 'First Last' with proper capitalization")
    private String name;

    @NotBlank(message = "Threshold is required")
    @Range(min = 1, max = 100, message = "Threshold must be between 1 and 100")
    private BigDecimal treshold;
}
