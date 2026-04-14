package com.grad.sam.model;

import com.grad.sam.enums.RiskRating;
import jakarta.persistence.*;
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

    @Column(name = "customer_ref", nullable = false, unique = true, length = 15)
    private String customerRef;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality", nullable = false, length = 2)
    private String nationality;                     // ISO 3166-1 alpha-2

    @Column(name = "country_of_residence", nullable = false, length = 2)
    private String countryOfResidence;

    @Column(name = "customer_type", nullable = false, length = 20)
    private String customerType;                    // INDIVIDUAL, CORPORATE, TRUST, CHARITY

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", nullable = false, length = 10)
    private RiskRating riskRating = RiskRating.LOW; // LOW, MEDIUM, HIGH

    @Column(name = "kyc_status", nullable = false, length = 15)
    private String kycStatus;                       // VERIFIED, PENDING, EXPIRED, BLOCKED

    @Column(name = "onboarded_date", nullable = false)
    private LocalDate onboardedDate;

    @Column(name = "is_pep", nullable = false)
    private Boolean isPep = false;                  // Politically Exposed Person

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Investigation> investigations;
}
