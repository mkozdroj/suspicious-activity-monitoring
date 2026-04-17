package com.grad.sam.controller;

import com.grad.sam.dto.response.WatchlistMatchDto;
import com.grad.sam.enums.WatchlistListType;
import com.grad.sam.model.Watchlist;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class WatchlistController {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistMatchRepository watchlistMatchRepository;

    // GET /api/v1/watchlist/search?name={name}
    @GetMapping("/watchlist/search")
    public ResponseEntity<List<Watchlist>> searchWatchlist(
            @RequestParam String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Query parameter 'name' is required and must not be blank");
        }

        List<Watchlist> matches = watchlistRepository
                .findByEntityNameContainingIgnoreCase(name.trim());
        return ResponseEntity.ok(matches);
    }

    // GET /api/v1/watchlist-matches
    @GetMapping("/watchlist-matches")
    public ResponseEntity<List<WatchlistMatchDto>> listWatchlistMatches(
            @RequestParam(required = false) Integer txnId) {

        List<WatchlistMatch> matches = txnId != null
                ? watchlistMatchRepository.findByTxn_TxnId(txnId)
                : watchlistMatchRepository.findAll();

        List<WatchlistMatchDto> dtos = matches.stream()
                .map(m -> new WatchlistMatchDto(
                        m.getMatchId(),
                        m.getWatchlist() != null
                                ? m.getWatchlist().getWatchlistId() : null,
                        m.getWatchlist() != null
                                ? m.getWatchlist().getEntityName() : null,
                        m.getWatchlist() != null
                                ? m.getWatchlist().getEntityType() : null,
                        m.getWatchlist() != null
                                && m.getWatchlist().getListType() != null
                                ? WatchlistListType.valueOf(
                                m.getWatchlist().getListType().name()) : null,
                        m.getMatchScore() != null
                                ? m.getMatchScore().toBigInteger() : null,
                        m.getMatchType(),
                        m.getStatus()))
                .toList();

        return ResponseEntity.ok(dtos);
    }
}