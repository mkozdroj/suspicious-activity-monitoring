package com.grad.sam.service;

import com.grad.sam.enums.MatchStatus;
import com.grad.sam.enums.MatchType;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.model.Txn;
import com.grad.sam.model.Watchlist;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.repository.WatchlistRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    private static final BigDecimal EXACT_MATCH_SCORE = new BigDecimal("100.00");
    private static final BigDecimal FUZZY_MATCH_SCORE = new BigDecimal("85.00");

    private final WatchlistRepository watchlistRepository;
    private final WatchlistMatchRepository watchlistMatchRepository;
    private final TxnRepository txnRepository;
    private final AlertService alertService;

    public WatchlistScreeningService(
            WatchlistRepository watchlistRepository,
            WatchlistMatchRepository watchlistMatchRepository,
            TxnRepository txnRepository,
            AlertService alertService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistMatchRepository = watchlistMatchRepository;
        this.txnRepository = txnRepository;
        this.alertService = alertService;
    }

    @Transactional
    public List<WatchlistMatch> screenCustomer(@NotNull String customerName, BigDecimal threshold, @NotNull Txn txn) throws IllegalStateException {
        log.info("Starting watchlist screening for txn: {}", txn.getTxnId());

        List<WatchlistMatch> matches = matchWatchlist(customerName, threshold, txn);

        if (matches.isEmpty()) {
            txnRepository.updateStatus(txn.getTxnId(), TxnStatus.SCREENED);
        } else {
            txnRepository.updateStatus(txn.getTxnId(), TxnStatus.PENDING);
            blockIfSanctioned(txn, matches);
        }

        return matches;
    }

    public void blockIfSanctioned(@NotNull Txn txn, List<WatchlistMatch> matches) {
        try {
            boolean sanctioned = matches.stream()
                    .anyMatch(m -> m.getMatchScore().compareTo(EXACT_MATCH_SCORE) == 0);

            if (sanctioned) {
                txnRepository.updateStatus(txn.getTxnId(), TxnStatus.BLOCKED);
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
