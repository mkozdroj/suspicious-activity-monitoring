package com.grad.sam.dto.response;

import com.grad.sam.enums.MatchStatus;
import com.grad.sam.enums.MatchType;
import com.grad.sam.enums.WatchlistEntityType;
import com.grad.sam.enums.WatchlistListType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistMatchDto {
    @NotNull(message = "Match ID is required")
    @Positive(message = "Match ID must be positive")
    private Integer matchId;

    @NotNull(message = "Watchlist ID is required")
    @Positive(message = "Watchlist ID must be positive")
    private Integer watchlistId;

    @NotBlank(message = "Entity name is required")
    private String entityName;

    @NotNull(message = "Entity type is required")
    private WatchlistEntityType entityType;

    @NotNull(message = "List source is required")
    private WatchlistListType listSource;

    @NotNull(message = "Match score is required")
    @Range(min = 0, max = 100, message = "Match score must be between 0 and 100")
    private BigInteger matchScore;

    @NotNull(message = "Match type is required")
    private MatchType matchType;

    @NotNull(message = "Match status is required")
    private MatchStatus status;
}
