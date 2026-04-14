package com.grad.sam.model;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "investigation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Investigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investigation_id")
    private Integer investigationId;

    @Column(name = "investigation_ref", nullable = false, unique = true, length = 15)
    private String investigationRef;

    // One investigation per alert (UNIQUE FK)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false, unique = true)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "opened_by", nullable = false, length = 60)
    private String openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20)
    private InvestigationOutcome outcome;       // set when investigation is resolved

    // LOW, MEDIUM, HIGH, URGENT
    @Column(name = "priority", nullable = false, length = 10)
    private String priority = "MEDIUM";

    @Column(name = "findings", length = 500)
    private String findings;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20)
    private InvestigationState state;
}
