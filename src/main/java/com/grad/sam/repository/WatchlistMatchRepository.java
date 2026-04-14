package com.grad.sam.repository;

import com.grad.sam.model.WatchlistMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WatchlistMatchRepository extends JpaRepository<WatchlistMatch, Integer> {

    List<WatchlistMatch> findByTxn_TxnId(Integer txnId);

    List<WatchlistMatch> findByTxn_TxnIdAndMatchScore(Integer txnId, BigDecimal matchScore);

    List<WatchlistMatch> findByWatchlist_WatchlistId(Integer watchlistId);

    List<WatchlistMatch> findByStatus(String status);

    List<WatchlistMatch> findByMatchType(String matchType);

    List<WatchlistMatch> findByReviewedBy(String reviewedBy);

    List<WatchlistMatch> findByTxn_TxnIdAndMatchScoreGreaterThanEqual(Integer txnId, BigDecimal threshold);
}
