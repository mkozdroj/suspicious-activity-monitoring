package com.grad.sam.model;

import com.grad.sam.enums.CustomerType;
import com.grad.sam.enums.KycStatus;
import com.grad.sam.enums.RiskRating;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @NotBlank
    @Size(max = 15)
    @Column(name = "customer_ref", nullable = false, unique = true, length = 15)
    private String customerRef;

    @NotBlank
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$", message = "Must be ISO 3166-1 alpha-2 code")
    @Column(name = "nationality", nullable = false, length = 2)
    private String nationality;                     // ISO 3166-1 alpha-2

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$", message = "Must be ISO 3166-1 alpha-2 code")
    @Column(name = "country_of_residence", nullable = false, length = 2)
    private String countryOfResidence;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", nullable = false, length = 10)
    private RiskRating riskRating = RiskRating.LOW;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 15)
    private KycStatus kycStatus;

    @NotNull
    @PastOrPresent
    @Column(name = "onboarded_date", nullable = false)
    private LocalDate onboardedDate;

    @NotNull
    @Column(name = "is_pep", nullable = false)
    private Boolean isPep = false;                 // Politically Exposed Person

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Investigation> investigations;
}
