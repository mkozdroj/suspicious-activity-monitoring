package com.grad.sam.dao;

import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.WatchlistMatchRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class WatchlistMatchDao {
    private final WatchlistMatchRepository watchlistMatchRepository;

    public WatchlistMatchDao(WatchlistMatchRepository watchlistMatchRepository) {
        this.watchlistMatchRepository = watchlistMatchRepository;
    }

    public List<WatchlistMatch> saveAll(List<WatchlistMatch> matches) {
        return watchlistMatchRepository.saveAll(matches);
    }

    public List<WatchlistMatch> findExactMatches(Integer txnId) {
        return watchlistMatchRepository.findByTxn_TxnIdAndMatchScore(txnId, new BigDecimal("100.00"));
    }

    // optional: generalized queries
    public List<WatchlistMatch> findByTxnIdAndScoreThreshold(Integer txnId, BigDecimal threshold) {
        return watchlistMatchRepository.findByTxnIdAndMatchScoreGreaterThanEqual(txnId, threshold);
    }
}
