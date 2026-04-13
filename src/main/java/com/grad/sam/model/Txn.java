package com.grad.sam.model;

import jakarta.persistence.*;
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
    private Long txnId;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 20)
    private String txnRef;

    @Column(name = "counterparty_account", length = 30)
    private String counterpartyAccount;

    @Column(name = "counterparty_bank", length = 60)
    private String counterpartyBank;

    @Column(name = "counterparty_country", length = 2)
    private String counterpartyCountry;

    @Column(name = "txn_type", nullable = false, length = 20)
    private String txnType;             // WIRE, CASH, CARD, INTERNAL, CRYPTO, CHEQUE

    @Column(name = "direction", nullable = false, length = 2)
    private String direction;                   // CR (credit/in) or DR (debit/out)

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountUsd;               // normalised to USD for rule evaluation

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "status", nullable = false, length = 12)
    private String status = "COMPLETED";        // COMPLETED, PENDING, REVERSED, FAILED

    @Column(name = "description", length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // One transaction can trigger many alerts
    @OneToMany(mappedBy = "txn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;

    // One transaction can match many watchlist entries
    @OneToMany(mappedBy = "txn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WatchlistMatch> watchlistMatches;
}
