package com.grad.sam.model;

import com.grad.sam.enums.AccountStatus;
import com.grad.sam.enums.AccountType;
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
    private Integer accountId;

    @NotBlank
    @Size(max = 40)
    @Column(name = "account_number", nullable = false, unique = true, length = 40)
    private String accountNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Must be ISO 4217 currency code")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Digits(integer = 16, fraction = 2)
    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @PastOrPresent
    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @NotBlank
    @Size(max = 10)
    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @Schema(hidden = true)
    private Customer customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(hidden = true)
    private List<Txn> txns;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(hidden = true)
    private List<Alert> alerts;
}
