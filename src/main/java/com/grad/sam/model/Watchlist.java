package com.grad.sam.model;

import com.grad.sam.enums.ListType;
import com.grad.sam.enums.WatchlistEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private ListType listType;

    @NotBlank
    @Size(max = 120)
    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private WatchlistEntityType entityType;

    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(name = "country", length = 2)
    private String country;

    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotBlank @Size(max = 200)
    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    @NotNull @PastOrPresent
    @Column(name = "listed_date", nullable = false)
    private LocalDate listedDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(hidden = true)
    private List<WatchlistMatch> matches;
}
