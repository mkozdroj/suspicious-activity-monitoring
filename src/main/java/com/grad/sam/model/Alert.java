package com.grad.sam.model;

import com.grad.sam.enums.AlertStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert", indexes = {
        @Index(name = "idx_alert_account", columnList = "account_id"),
        @Index(name = "idx_alert_status",  columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer alertId;

    @NotBlank
    @Size(max = 15)
    @Column(name = "alert_ref", nullable = false, unique = true, length = 15)
    private String alertRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule alertRule;

    // Alert is raised at account level (not just transaction level)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Specific transaction that triggered the alert (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txn_id")
    private Txn txn;

    @NotNull
    @PastOrPresent
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    // 0–100 risk score (replaces severity enum — aligns with your schema)
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "alert_score", nullable = false)
    private Short alertScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private AlertStatus status = AlertStatus.OPEN;

    @Size(max = 60)
    @Column(name = "assigned_to", length = 60)
    private String assignedTo;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    // Inverse side — investigation owns the FK to alert
    @OneToOne(mappedBy = "alert", fetch = FetchType.LAZY)
    private Investigation investigation;
}
