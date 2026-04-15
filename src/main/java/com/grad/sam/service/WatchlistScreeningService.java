package com.grad.sam.service;

import com.grad.sam.dao.TxnDao;
import com.grad.sam.model.Txn;
import com.grad.sam.model.Watchlist;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.repository.WatchlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WatchlistScreeningService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistMatchRepository watchlistMatchRepository;
    private final TxnDao transactionDao;

    public WatchlistScreeningService(
            WatchlistRepository watchlistRepository,
            WatchlistMatchRepository watchlistMatchRepository,
            TxnDao transactionDao
    ) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistMatchRepository = watchlistMatchRepository;
        this.transactionDao = transactionDao;
    }

    @Transactional
    public List<WatchlistMatch> screenCustomer(String customerName, BigDecimal threshold, Txn txn) throws IllegalStateException {
        log.info("Starting watchlist screening for txn: {}", txn.getTxnId());

        List<WatchlistMatch> matches = matchWatchlist(customerName, threshold, txn);

        String finalStatus = "SCREENED";
        boolean hasExactMatch = matches.stream()
                .anyMatch(m -> m.getMatchScore().compareTo(new BigDecimal("100.00")) == 0);
        if (hasExactMatch)
            finalStatus = "BLOCKED";
        else if (!matches.isEmpty())
            finalStatus = "PENDING";

        transactionDao.updateStatus(txn.getTxnId(), finalStatus);
        if (finalStatus.equals("BLOCKED"))
            log.warn("Transaction {} BLOCKED — exact watchlist match found", txn.getTxnId());

        return matches;
    }

    @Transactional
    public List<WatchlistMatch> matchWatchlist(String customerName, BigDecimal threshold, Txn txn) {
        String normalized = customerName.toUpperCase().trim();
        log.info("Screening customer: {} against watchlist (threshold: {})", normalized, threshold);

        List<Watchlist> activeEntries = watchlistRepository.findByIsActive(true);
        List<WatchlistMatch> results = new ArrayList<>();

        for (Watchlist entry : activeEntries) {
            String entryName = entry.getEntityName().toUpperCase().trim();
            BigDecimal score = BigDecimal.ZERO;

            if (entryName.equals(normalized)) {
                score = new BigDecimal("100.00");
            } else if (normalized.contains(entryName) || entryName.contains(normalized)) {
                score = new BigDecimal("85.00");
            }

            if (score.compareTo(threshold) >= 0) {
                WatchlistMatch match = new WatchlistMatch();
                match.setTxn(txn);
                match.setWatchlist(entry);
                match.setMatchType("FUZZY_NAME");
                match.setMatchScore(score);
                match.setMatchedField("entity_name");
                match.setMatchedValue(customerName);
                match.setStatus("PENDING");
                results.add(match);
            }
        }

        watchlistMatchRepository.saveAll(results);
        log.info("Found {} watchlist match(es) for: {}", results.size(), customerName);
        return results;
    }
}
