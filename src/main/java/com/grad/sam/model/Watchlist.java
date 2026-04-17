package com.grad.sam.model;

import com.grad.sam.enums.WatchlistEntityType;
import com.grad.sam.enums.WatchlistListType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "watchlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_id")
    private Integer watchlistId;

    // OFAC, UN, EU, HMT, INTERPOL, INTERNAL, PEP
    @Column(name = "list_type", nullable = false, length = 20)
    private WatchlistListType listType;

    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    // INDIVIDUAL, ENTITY, VESSEL, AIRCRAFT
    @Column(name = "entity_type", nullable = false, length = 20)
    private WatchlistEntityType entityType;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    @Column(name = "listed_date", nullable = false)
    private LocalDate listedDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WatchlistMatch> matches;
}
