package com.grad.sam.model;

import com.grad.sam.enums.MatchStatus;
import com.grad.sam.enums.MatchType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_match")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Integer matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txn_id", nullable = false)
    private Txn txn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;

    @NotBlank
    @Size(max = 50)
    @Column(name = "matched_field", nullable = false, length = 50)
    private String matchedField;

    @NotBlank @Size(max = 120)
    @Column(name = "matched_value", nullable = false, length = 120)
    private String matchedValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private MatchStatus status = MatchStatus.PENDING;

    @Size(max = 60)
    @Column(name = "reviewed_by", length = 60)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
