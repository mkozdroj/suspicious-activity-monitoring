package com.grad.sam.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "account", indexes = {
        @Index(name = "idx_account_customer", columnList = "customer_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_number", nullable = false, unique = true, length = 40)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;             // CURRENT, SAVINGS, TRADING, CUSTODY, CORRESPONDENT

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate;

    @Column(name = "status", nullable = false, length = 10)
    private String status = "ACTIVE";       // ACTIVE, FROZEN, CLOSED, RESTRICTED

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // One account can have many transactions — FK is on transaction side
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    // One account can have many alerts — FK is on alert side
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;
}
