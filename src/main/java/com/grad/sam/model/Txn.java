package com.grad.sam.model;

import com.grad.sam.enums.TxnDirection;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.enums.TxnType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "txn", indexes = {
        @Index(name = "idx_txn_account",  columnList = "account_id"),
        @Index(name = "idx_txn_date",     columnList = "txn_date"),
        @Index(name = "idx_txn_amount",   columnList = "amount_usd"),
        @Index(name = "idx_txn_cc",       columnList = "counterparty_country")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Txn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Integer txnId;

    @NotBlank
    @Size(max = 20)
    @Column(name = "txn_ref", nullable = false, unique = true, length = 20)
    private String txnRef;

    @Size(max = 30)
    @Column(name = "counterparty_account", length = 30)
    private String counterpartyAccount;

    @Size(max = 60)
    @Column(name = "counterparty_bank", length = 60)
    private String counterpartyBank;

    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(name = "counterparty_country", length = 2)
    private String counterpartyCountry;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private TxnType txnType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 2)
    private TxnDirection direction;

    @NotNull
    @Positive
    @Digits(integer = 16, fraction = 2)
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Positive
    @Digits(integer = 16, fraction = 2)
    @Column(name = "amount_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountUsd;

    @NotNull
    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @NotNull
    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private TxnStatus status = TxnStatus.COMPLETED;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @Schema(hidden = true)
    private Account account;

    @OneToMany(mappedBy = "txn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(hidden = true)
    private List<Alert> alerts;

    @OneToMany(mappedBy = "txn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(hidden = true)
    private List<WatchlistMatch> watchlistMatches;
}
