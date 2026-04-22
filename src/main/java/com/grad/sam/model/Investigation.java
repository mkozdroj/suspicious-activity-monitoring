package com.grad.sam.model;

import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.enums.Priority;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(max = 15)
    @Column(name = "investigation_ref", nullable = false, unique = true, length = 15)
    private String investigationRef;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false, unique = true)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank
    @Size(max = 60)
    @Column(name = "opened_by", nullable = false, length = 60)
    private String openedBy;

    @NotNull
    @PastOrPresent
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @PastOrPresent
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20)
    private InvestigationOutcome outcome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    @Size(max = 500)
    @Column(name = "findings", length = 500)
    private String findings;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 15)
    private InvestigationState state = InvestigationState.OPEN;
}
