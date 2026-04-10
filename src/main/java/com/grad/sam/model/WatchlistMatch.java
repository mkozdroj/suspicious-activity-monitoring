package com.grad.sam.model;

import jakarta.persistence.*;
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
    private Long matchId;

    // Match is raised at transaction level (not customer level)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    // NAME, ACCOUNT, COUNTRY, FUZZY_NAME
    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;

    // 0.00 – 100.00 confidence score
    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(name = "matched_field", nullable = false, length = 50)
    private String matchedField;

    @Column(name = "matched_value", nullable = false, length = 120)
    private String matchedValue;

    // PENDING, FALSE_POSITIVE, CONFIRMED, ESCALATED
    @Column(name = "status", nullable = false, length = 15)
    private String status = "PENDING";

    @Column(name = "reviewed_by", length = 60)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
