package com.grad.sam.service;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.enums.Priority;
import com.grad.sam.exception.AlertNotFoundException;
import com.grad.sam.exception.CustomerNotFoundException;
import com.grad.sam.exception.DuplicateInvestigationException;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.exception.InvalidStateTransitionException;
import com.grad.sam.exception.InvestigationNotFoundException;
import com.grad.sam.exception.MissingFindingsException;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.InvestigationRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@Validated
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

    private final InvestigationRepository investigationRepository;
    private final AlertRepository alertRepository;

    public InvestigationService(InvestigationRepository investigationRepository,
                                AlertRepository alertRepository) {
        this.investigationRepository = investigationRepository;
        this.alertRepository = alertRepository;
    }

    // -------------------------------------------------------------------------
    // Open Case
    // -------------------------------------------------------------------------

    /**
     * Opens a new investigation case for a triggered alert and assigns it to a
     * compliance officer.
     *
     * <p>Each alert may have at most one investigation (enforced by the UNIQUE
     * constraint on {@code investigation.alert_id}).</p>
     *
     * @param alertId          the ID of the alert that prompted the investigation; must be a positive integer
     * @param assignedOfficer  email / username of the compliance officer taking the case; must not be blank
     * @param priority         case priority — LOW, MEDIUM, HIGH, URGENT; must not be blank
     * @return the persisted {@link Investigation}
     * @throws jakarta.validation.ConstraintViolationException if any parameter fails its constraint
     * @throws InvalidInputException                           if {@code priority} is not a recognised
     *                                                         {@link Priority} value
     * @throws AlertNotFoundException                          if no alert exists for the given ID
     * @throws DuplicateInvestigationException                 if an investigation already exists for the alert
     * @throws CustomerNotFoundException                       if the alert's account has no associated customer
     */
    @Transactional
    public Investigation openCase(@NotNull @Positive Integer alertId,
                                  @NotBlank String assignedOfficer,
                                  @NotBlank String priority) {

        Priority resolvedPriority = resolvePriority(priority);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        if (investigationRepository.findByAlert_AlertId(alertId).isPresent()) {
            throw new DuplicateInvestigationException(alertId);
        }

        Customer customer = alert.getAccount().getCustomer();
        if (customer == null) {
            throw new CustomerNotFoundException(alertId);
        }

        Investigation investigation = new Investigation();
        investigation.setAlert(alert);
        investigation.setCustomer(customer);
        investigation.setOpenedBy(assignedOfficer);
        investigation.setOpenedAt(LocalDateTime.now());
        investigation.setPriority(resolvedPriority);
        investigation.setState(InvestigationState.OPEN);

        // Move the alert into UNDER_REVIEW so it doesn't appear on the open alerts view
        alert.setStatus(AlertStatus.UNDER_REVIEW);
        alert.setAssignedTo(assignedOfficer);
        alertRepository.save(alert);

        Investigation saved = investigationRepository.save(investigation);
        log.info("Opened investigation {} for alert {} — assigned to {} (priority: {})",
                saved.getInvestigationRef(), alertId, assignedOfficer, resolvedPriority);

        saved.setInvestigationRef(buildRef(saved.getInvestigationId()));
        investigationRepository.save(saved);

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
     *   <li>UNDER_REVIEW → CLOSED (requires both {@code outcome} and non-blank {@code findings})</li>
     * </ul>
     *
     * <p>On CLOSED, {@code closedAt} is stamped and the linked alert status is updated
     * to reflect the outcome (e.g. {@code SAR_FILED}, {@code NO_ACTION}).</p>
     *
     * @param investigationId  the ID of the investigation to update; must be a positive integer
     * @param newState         the target state; must not be null
     * @param outcome          required when closing; ignored for other transitions
     * @param findings         compliance officer's findings summary; required when closing
     * @return the updated {@link Investigation}
     * @throws jakarta.validation.ConstraintViolationException if {@code investigationId} or
     *                                                         {@code newState} fails its constraint
     * @throws InvestigationNotFoundException                  if no investigation exists for the given ID
     * @throws InvalidStateTransitionException                 if the requested transition is not permitted
     *                                                         by the state machine
     * @throws InvalidInputException                           if closing without an outcome
     * @throws MissingFindingsException                        if closing without a findings summary
     */
    @Transactional
    public Investigation updateCaseStatus(@NotNull @Positive Integer investigationId,
                                          @NotNull InvestigationState newState,
                                          InvestigationOutcome outcome,
                                          String findings) {

        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new InvestigationNotFoundException(investigationId));

        InvestigationState currentState = investigation.getState();

        if (!isValidTransition(currentState, newState)) {
            throw new InvalidStateTransitionException(
                    investigation.getInvestigationRef(), currentState, newState);
        }

        if (newState == InvestigationState.CLOSED) {
            validateCloseInputs(investigation, outcome, findings);

            investigation.setOutcome(outcome);
            investigation.setClosedAt(LocalDateTime.now());
            investigation.setFindings(findings);
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
     * @return list of investigations in state {@link InvestigationState#OPEN}
     */
    public List<Investigation> findOpenCases() {
        return investigationRepository.findByState(InvestigationState.OPEN);
    }

    /**
     * Returns all investigations currently under review.
     *
     * @return list of investigations in state {@link InvestigationState#UNDER_REVIEW}
     */
    public List<Investigation> findCasesUnderReview() {
        return investigationRepository.findByState(InvestigationState.UNDER_REVIEW);
    }

    /**
     * Returns all investigations assigned to a specific compliance officer.
     *
     * @param officerEmail the officer's email / username; must not be blank
     * @return list of investigations opened by the given officer
     * @throws jakarta.validation.ConstraintViolationException if {@code officerEmail} is blank
     */
    public List<Investigation> findByOfficer(@NotBlank String officerEmail) {
        return investigationRepository.findByOpenedBy(officerEmail);
    }

    /**
     * Finds an investigation by its unique reference (e.g. {@code INV-260414-00001}).
     *
     * @param ref the investigation reference; must not be blank
     * @return the matching {@link Investigation}
     * @throws jakarta.validation.ConstraintViolationException if {@code ref} is blank
     * @throws InvestigationNotFoundException                  if no investigation matches the reference
     */
    public Investigation findByRef(@NotBlank String ref) {
        return investigationRepository.findByInvestigationRef(ref)
                .orElseThrow(() -> new InvestigationNotFoundException(ref));
    }

    // -------------------------------------------------------------------------
    // Private validation helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a priority string to a {@link Priority} enum value.
     *
     * @param priority the raw priority string; assumed non-blank (enforced by {@code @NotBlank})
     * @throws InvalidInputException if the string does not match any {@link Priority} constant
     */
    private Priority resolvePriority(String priority) {
        try {
            return Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(
                    "Invalid investigation priority: '" + priority + "'. " +
                            "Accepted values are: LOW, MEDIUM, HIGH, URGENT");
        }
    }

    /**
     * Validates that a close operation supplies both an outcome and a findings summary.
     *
     * @throws InvalidInputException    if {@code outcome} is null
     * @throws MissingFindingsException if {@code findings} is null or blank
     */
    private void validateCloseInputs(Investigation investigation,
                                     InvestigationOutcome outcome,
                                     String findings) {
        if (outcome == null) {
            throw new InvalidInputException(
                    "Outcome is required when closing investigation "
                            + investigation.getInvestigationRef()
                            + ". Valid outcomes: SAR_FILED, NO_ACTION, ACCOUNT_CLOSED, ESCALATED, MONITORING.");
        }
        if (findings == null || findings.isBlank()) {
            throw new MissingFindingsException(investigation.getInvestigationRef());
        }
    }

    // -------------------------------------------------------------------------
    // Private helper methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the transition from {@code from} to {@code to} is
     * permitted by the state machine.
     */
    private boolean isValidTransition(InvestigationState from, InvestigationState to) {
        Set<InvestigationState> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        return allowed.contains(to);
    }

    /**
     * Updates the linked alert's status when an investigation is closed.
     *
     * <p>The alert status mirrors the investigation outcome so that reporting
     * queries (e.g. SAR pipeline) can join directly on {@code alert.status}.</p>
     */
    private void syncAlertOnClose(Alert alert, InvestigationOutcome outcome) {
        if (alert == null) return;

        AlertStatus alertStatus = switch (outcome) {
            case SAR_FILED                      -> AlertStatus.SAR_FILED;
            case NO_ACTION, ACCOUNT_CLOSED      -> AlertStatus.CLOSED;
            case ESCALATED                      -> AlertStatus.ESCALATED;
            case MONITORING                     -> AlertStatus.UNDER_REVIEW;
        };

        alert.setStatus(alertStatus);
        alertRepository.save(alert);
        log.debug("Alert {} status updated to {} following investigation close (outcome: {})",
                alert.getAlertRef(), alertStatus, outcome);
    }

    private String buildRef(Integer id) {
        String datePart = LocalDateTime.now().format(REF_DATE_FMT);
        return String.format("INV-%s-%05d", datePart, id);
    }
}
