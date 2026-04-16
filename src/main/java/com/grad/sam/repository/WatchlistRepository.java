package com.grad.sam.repository;

import com.grad.sam.enums.WatchlistEntityType;
import com.grad.sam.enums.WatchlistListType;
import com.grad.sam.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Integer> {

    List<Watchlist> findByListType(WatchlistListType listType);

    List<Watchlist> findByEntityType(WatchlistEntityType entityType);

    List<Watchlist> findByIsActive(Boolean isActive);

    List<Watchlist> findByCountry(String country);

    List<Watchlist> findByEntityNameContainingIgnoreCase(String name);
}
