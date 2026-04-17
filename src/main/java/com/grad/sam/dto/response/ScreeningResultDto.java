package com.grad.sam.dto.response;

import com.grad.sam.enums.TxnStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResultDto {
    @NotBlank(message = "Transaction ID is required")
    @Positive(message = "Transaction ID must be positive")
    private Integer txnId;

    @NotBlank(message = "Transaction reference is required")
    @Positive(message = "Transaction reference must be positive")
    private String txnRef;

    @NotBlank(message = "Customer name is required")
    @Pattern(regexp = "^[A-Z][a-zA-Z'-]+ [A-Z][a-zA-Z'-]+$", message = "Customer name must be in the format 'First Last' with proper capitalization")
    private String customerName;

    @NotBlank(message = "Account number is required")
    private TxnStatus txnStatus;

    @NotBlank(message = "Match count is required")
    @Positive(message = "Match count must be positive")
    private Integer matchCount;

    @NotNull(message = "Alerts list is required")
    private List<AlertSummaryDto> alerts;

    @NotNull(message = "Watchlist matches list is required")
    private List<WatchlistMatchDto> watchlistMatches;
}
