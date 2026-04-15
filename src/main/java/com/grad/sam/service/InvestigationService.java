package com.grad.sam.service;

import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.CustomerRepository;
import com.grad.sam.repository.InvestigationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service that manages the AML investigation (case management) lifecycle.
 *
 * <p>When an alert is raised by the rule engine, a compliance officer opens an
 * investigation case to determine whether the suspicious activity is genuine.
 * The case then moves through a defined state machine:</p>
 *
 * <pre>
 *   OPEN ──► UNDER_REVIEW ──► CLOSED
 * </pre>
 *
 * <p>Domain context: Investigations are the primary vehicle for SAR (Suspicious
 * Activity Report) filing decisions.  A case that reaches CLOSED with outcome
 * {@code SAR_FILED} triggers regulatory reporting obligations (e.g. FINTRAC,
 * FinCEN, NCA).  All state transitions are idempotent and auditable.</p>
 */
@Slf4j
@Service
public class InvestigationService {

    /**
     * Allowed state transitions: key → set of valid next states.
     */
    private static final Map<InvestigationState, Set<InvestigationState>> ALLOWED_TRANSITIONS =
            Map.of(
                    InvestigationState.OPEN,         Set.of(InvestigationState.UNDER_REVIEW),
                    InvestigationState.UNDER_REVIEW,  Set.of(InvestigationState.CLOSED),
                    InvestigationState.CLOSED,        Set.of()   // terminal state
            );

    private static final DateTimeFormatter REF_DATE_FMT =
            DateTimeFormatter.ofPattern("yyMMdd");

    /** Simple in-process sequence for reference generation (sufficient for single-node). */
    private static final AtomicLong REF_SEQUENCE = new AtomicLong(1);

    private final InvestigationRepository investigationRepository;
    private final AlertRepository alertRepository;
    private final CustomerRepository customerRepository;

    public InvestigationService(InvestigationRepository investigationRepository,
                                AlertRepository alertRepository,
                                CustomerRepository customerRepository) {
        this.investigationRepository = investigationRepository;
        this.alertRepository = alertRepository;
        this.customerRepository = customerRepository;
    }

    // -------------------------------------------------------------------------
    // Open Case
    // -------------------------------------------------------------------------

    /**
     * Opens a new investigation case for a triggered alert and assigns it to a
     * compliance officer.
     *
     * <p>Each alert may have at most one investigation (enforced by the UNIQUE
     * constraint on {@code investigation.alert_id}).  If an investigation already
     *
     * @param alertId          the ID of the alert that prompted the investigation
     * @param assignedOfficer  email / username of the compliance officer taking the case
     * @param priority         case priority: LOW, MEDIUM, HIGH, URGENT
     * @return the persisted {@link Investigation}
     */
    @Transactional
    public Investigation openCase(Integer alertId, String assignedOfficer, String priority) {

        Alert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null) {
            log.warn("Alert not found: {}", alertId);
            return null;
        }

        if (investigationRepository.findByAlert_AlertId(alertId).isPresent()) {
            log.warn("Investigation already exists for alert {}", alertId);
            return null;
        }

        Customer customer = customerRepository
                .findById(alert.getAccount().getCustomer().getCustomerId()).orElse(null);
        if (customer == null) {
            log.warn("Customer not found for alert: {}", alertId);
            return null;
        }

        Investigation investigation = new Investigation();
        investigation.setInvestigationRef(generateRef());
        investigation.setAlert(alert);
        investigation.setCustomer(customer);
        investigation.setOpenedBy(assignedOfficer);
        investigation.setOpenedAt(LocalDateTime.now());
        investigation.setPriority(priority != null ? priority : "MEDIUM");
        investigation.setState(InvestigationState.OPEN);

        // Move the alert into UNDER_REVIEW so it doesn't appear on the open alerts view
        alert.setStatus("UNDER_REVIEW");
        alert.setAssignedTo(assignedOfficer);
        alertRepository.save(alert);

        Investigation saved = investigationRepository.save(investigation);
        log.info("Opened investigation {} for alert {} — assigned to {}",
                saved.getInvestigationRef(), alertId, assignedOfficer);

        return saved;
    }

    // -------------------------------------------------------------------------
    // Update Case Status
    // -------------------------------------------------------------------------

    /**
     * Moves an investigation through its lifecycle state machine.
     *
     * <p>Valid transitions:
     * <ul>
     *   <li>OPEN → UNDER_REVIEW</li>
     *   <li>UNDER_REVIEW → CLOSED (requires {@code outcome} and {@code findings})</li>
     * </ul>
     *
     * <p>On CLOSED, {@code closedAt} is stamped and the linked alert status is updated
     * to reflect the outcome (e.g. {@code SAR_FILED}, {@code NO_ACTION}).</p>
     *
     * @param investigationId  the ID of the investigation to update
     * @param newState         the target state
     * @param outcome          required when closing; ignored for other transitions
     * @param findings         compliance officer's findings summary (recommended on close)
     * @return the updated {@link Investigation}
     */
    @Transactional
    public Investigation updateCaseStatus(Integer investigationId,
                                          InvestigationState newState,
                                          InvestigationOutcome outcome,
                                          String findings) {

        Investigation investigation = investigationRepository.findById(investigationId).orElse(null);
        if (investigation == null) {
            log.warn("Investigation not found: {}", investigationId);
            return null;
        }

        InvestigationState currentState = investigation.getState();

        if (!isValidTransition(currentState, newState)) {
            log.warn("Invalid state transition for investigation {}: {} → {}",
                    investigation.getInvestigationRef(), currentState, newState);
            return null;
        }

        if (newState == InvestigationState.CLOSED) {
            if (outcome == null) {
                log.warn("Outcome is required when closing investigation {}",
                        investigation.getInvestigationRef());
                return null;
            }
            investigation.setOutcome(outcome);
            investigation.setClosedAt(LocalDateTime.now());
            if (findings != null && !findings.isBlank()) {
                investigation.setFindings(findings);
            }
            syncAlertOnClose(investigation.getAlert(), outcome);
        }

        investigation.setState(newState);
        Investigation saved = investigationRepository.save(investigation);

        log.info("Investigation {} transitioned {} → {} (outcome: {})",
                saved.getInvestigationRef(), currentState, newState, outcome);

        return saved;
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    /**
     * Returns all open investigations — cases awaiting assignment.
     *
     * @return list of investigations in state OPEN
     */
    public List<Investigation> findOpenCases() {
        return investigationRepository.findByState(InvestigationState.OPEN);
    }

    /**
     * Returns all investigations currently under review.
     *
     * @return list of investigations in state UNDER_REVIEW
     */
    public List<Investigation> findCasesUnderReview() {
        return investigationRepository.findByState(InvestigationState.UNDER_REVIEW);
    }

    /**
     * Returns all investigations assigned to a specific compliance officer.
     *
     * @param officerEmail the officer's email / username
     * @return list of investigations opened by the given officer
     */
    public List<Investigation> findByOfficer(String officerEmail) {
        return investigationRepository.findByOpenedBy(officerEmail);
    }

    /**
     * Finds an investigation by its unique reference.
     *
     * @param ref the investigation reference (e.g. INV-260414-00001)
     * @return the investigation
     */
    public Investigation findByRef(String ref) {
        return investigationRepository.findByInvestigationRef(ref).orElse(null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that the requested state transition is permitted by the state machine.
     *
     * @param from the current state
     * @param to   the requested next state
     //* @param ref  investigation reference for error messaging
     */
    private boolean isValidTransition(InvestigationState from, InvestigationState to) {
        Set<InvestigationState> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        return allowed.contains(to);
    }

    /**
     * Updates the linked alert's status when an investigation is closed.
     *
     * <p>The alert status mirrors the investigation outcome so that reporting
     * queries (e.g. SAR pipeline) can join directly on alert.status.</p>
     */
    private void syncAlertOnClose(Alert alert, InvestigationOutcome outcome) {
        if (alert == null) return;

        String alertStatus = switch (outcome) {
            case SAR_FILED      -> "SAR_FILED";
            case NO_ACTION      -> "CLOSED";
            case ACCOUNT_CLOSED -> "CLOSED";
            case ESCALATED      -> "ESCALATED";
            case MONITORING     -> "UNDER_REVIEW";
        };

        alert.setStatus(alertStatus);
        alertRepository.save(alert);
        log.debug("Alert {} status updated to {} following investigation close (outcome: {})",
                alert.getAlertRef(), alertStatus, outcome);
    }

    /**
     * Generates a unique investigation reference in the format {@code INV-YYMMDD-NNNNN}.
     *
     * @return unique reference string
     */
    private String generateRef() {
        String datePart = LocalDateTime.now().format(REF_DATE_FMT);
        long seq = REF_SEQUENCE.getAndIncrement();
        return String.format("INV-%s-%05d", datePart, seq);
    }
}
