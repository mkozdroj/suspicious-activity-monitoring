package com.grad.sam.service;

import com.grad.sam.enums.MatchStatus;
import com.grad.sam.enums.MatchType;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.Txn;
import com.grad.sam.model.Watchlist;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.repository.WatchlistRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Validated
public class WatchlistScreeningService {
    public static final BigDecimal EXACT_MATCH_SCORE = new BigDecimal("100.00");
    public static final BigDecimal FUZZY_MATCH_SCORE = new BigDecimal("85.00");

    private final WatchlistRepository watchlistRepository;
    private final WatchlistMatchRepository watchlistMatchRepository;
    private final TxnService txnService;
    private final AlertService alertService;

    public WatchlistScreeningService(
            WatchlistRepository watchlistRepository,
            WatchlistMatchRepository watchlistMatchRepository,
            TxnService txnService,
            AlertService alertService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistMatchRepository = watchlistMatchRepository;
        this.txnService = txnService;
        this.alertService = alertService;
    }

    @Transactional
    public List<WatchlistMatch> screenCustomer(@NotBlank String customerName, BigDecimal threshold, @NotBlank Txn txn) throws IllegalStateException {
        log.info("Starting watchlist screening for txn: {}", txn.getTxnId());

        List<WatchlistMatch> matches = matchWatchlist(customerName, threshold, txn);

        if (matches.isEmpty()) {
            txnService.updateTxnStatus(txn.getTxnId(), TxnStatus.SCREENED);
        } else {
            txnService.updateTxnStatus(txn.getTxnId(), TxnStatus.PENDING);
            blockIfSanctioned(txn, matches);
        }

        return matches;
    }

    public void blockIfSanctioned(@NotBlank Txn txn, List<WatchlistMatch> matches) {
        try {
            boolean sanctioned = matches.stream()
                    .anyMatch(m -> m.getMatchScore().compareTo(EXACT_MATCH_SCORE) == 0);

            if (sanctioned) {
                txnService.updateTxnStatus(txn.getTxnId(), TxnStatus.BLOCKED);
                log.warn("Transaction {} BLOCKED — exact watchlist match found", txn.getTxnId());
                alertService.raiseAlert(txn.getTxnId(), "WATCHLIST", "Transaction blocked due to exact watchlist match");
            }

        } catch (Exception e) {
            log.error("Unexpected error in blockIfSanctioned for txn: {}", txn.getTxnId(), e);
            throw new RuntimeException("Failed to evaluate sanction status", e);
        }
    }

    private List<WatchlistMatch> matchWatchlist(String customerName, BigDecimal threshold, Txn txn) {
        String normalized = customerName.toUpperCase().trim();
        log.info("Screening customer: {} against watchlist (threshold: {})", normalized, threshold);

        List<Watchlist> activeEntries = watchlistRepository.findByIsActive(true);
        List<WatchlistMatch> results = new ArrayList<>();

        for (Watchlist entry : activeEntries) {
            String entryName = entry.getEntityName().toUpperCase().trim();
            BigDecimal score = BigDecimal.ZERO;

            if (entryName.equals(normalized)) {
                score = EXACT_MATCH_SCORE;
            } else if (normalized.contains(entryName) || entryName.contains(normalized)) {
                score = FUZZY_MATCH_SCORE;
            }

            if (score.compareTo(threshold) >= 0) {
                WatchlistMatch match = new WatchlistMatch();
                match.setTxn(txn);
                match.setWatchlist(entry);
                match.setMatchType(MatchType.FUZZY_NAME);
                match.setMatchScore(score);
                match.setMatchedField("entity_name");
                match.setMatchedValue(customerName);
                match.setStatus(MatchStatus.PENDING);
                results.add(match);
            }
        }

        watchlistMatchRepository.saveAll(results);
        log.info("Found {} watchlist match(es) for: {}", results.size(), customerName);
        return results;
    }
}
