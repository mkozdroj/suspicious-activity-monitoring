package com.grad.sam.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_txn_account",  columnList = "account_id"),
        @Index(name = "idx_txn_date",     columnList = "transaction_date"),
        @Index(name = "idx_txn_amount",   columnList = "amount_usd"),
        @Index(name = "idx_txn_cc",       columnList = "counterparty_country")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 20)
    private String transactionRef;

    @Column(name = "counterparty_account", length = 30)
    private String counterpartyAccount;

    @Column(name = "counterparty_bank", length = 60)
    private String counterpartyBank;

    @Column(name = "counterparty_country", length = 2)
    private String counterpartyCountry;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;             // WIRE, CASH, CARD, INTERNAL, CRYPTO, CHEQUE

    @Column(name = "direction", nullable = false, length = 2)
    private String direction;                   // CR (credit/in) or DR (debit/out)

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountUsd;               // normalised to USD for rule evaluation

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

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
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;

    // One transaction can match many watchlist entries
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WatchlistMatch> watchlistMatches;
}
