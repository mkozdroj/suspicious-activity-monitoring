package com.grad.sam.dao;

import com.grad.sam.model.Watchlist;
import com.grad.sam.repository.WatchlistRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class WatchlistDao {
    private final WatchlistRepository watchlistRepository;
    public WatchlistDao(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
    }

    public List<Watchlist> findByIsActive(Boolean isActive) {
        return watchlistRepository.findByIsActive(isActive);
    }

    public Optional<Watchlist> findById(Integer id) {
        return watchlistRepository.findById(id);
    }

    public Watchlist save(Watchlist watchlist) {
        Watchlist saved = watchlistRepository.save(watchlist);
        log.info("Saved watchlist entry: {} ({})", saved.getEntityName(), saved.getWatchlistId());
        return saved;
    }
}