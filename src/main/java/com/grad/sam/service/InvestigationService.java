package com.grad.sam.service;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.enums.Priority;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.exception.InvalidInputException;
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

@Slf4j
@Service
@Validated
public class InvestigationService {

    private static final Map<InvestigationState, Set<InvestigationState>> ALLOWED_TRANSITIONS =
            Map.of(
                    InvestigationState.OPEN,         Set.of(InvestigationState.UNDER_REVIEW),
                    InvestigationState.UNDER_REVIEW,  Set.of(InvestigationState.CLOSED),
                    InvestigationState.CLOSED,        Set.of()
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

    // Open Case

    @Transactional
    public Investigation openCase(@NotNull @Positive Integer alertId,
                                  @NotBlank String assignedOfficer,
                                  @NotNull Priority priority) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new DataNotFoundException(
                        "Alert not found for id: " + alertId));

        if (investigationRepository.findByAlert_AlertId(alertId).isPresent()) {
            throw new DataNotFoundException(
                    "Investigation already exists for alert id: " + alertId);
        }

        Customer customer = alert.getAccount().getCustomer();
        if (customer == null) {
            throw new DataNotFoundException(
                    "No customer found for the account linked to alert id: " + alertId
                            + ". Cannot open investigation without a valid customer.");
        }

        Investigation investigation = new Investigation();
        investigation.setAlert(alert);
        investigation.setCustomer(customer);
        investigation.setOpenedBy(assignedOfficer);
        investigation.setOpenedAt(LocalDateTime.now());
        investigation.setPriority(priority);
        investigation.setState(InvestigationState.OPEN);
        investigation.setInvestigationRef(buildTemporaryRef());

        alert.setStatus(AlertStatus.UNDER_REVIEW);
        alert.setAssignedTo(assignedOfficer);
        alertRepository.save(alert);

        Investigation saved = investigationRepository.save(investigation);
        log.info("Opened investigation {} for alert {} — assigned to {}",
                saved.getInvestigationRef(), alertId, assignedOfficer);

        saved.setInvestigationRef(buildRef(saved.getInvestigationId()));
        investigationRepository.save(saved);

        return saved;
    }

    // Update Case Status

    @Transactional
    public Investigation updateCaseStatus(@NotNull @Positive Integer investigationId,
                                          @NotNull InvestigationState newState,
                                          InvestigationOutcome outcome,
                                          String findings) {

        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new DataNotFoundException(
                        "Investigation not found for id: " + investigationId));

        InvestigationState currentState = investigation.getState();

        if (!isValidTransition(currentState, newState)) {
            throw new DataNotFoundException(
                    "Invalid state transition for investigation " + investigation.getInvestigationRef()
                            + ": " + currentState + " → " + newState);
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

    // Query helpers

    public List<Investigation> findOpenCases() {
        return investigationRepository.findByState(InvestigationState.OPEN);
    }

    public List<Investigation> findCasesUnderReview() {
        return investigationRepository.findByState(InvestigationState.UNDER_REVIEW);
    }

    public List<Investigation> findByOfficer(@NotBlank String officerEmail) {
        return investigationRepository.findByOpenedBy(officerEmail);
    }

    public Investigation findByRef(@NotBlank String ref) {
        return investigationRepository.findByInvestigationRef(ref)
                .orElseThrow(() -> new DataNotFoundException(
                        "Investigation not found for ref: " + ref));
    }

    // Private validation helpers

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
            throw new InvalidInputException(
                    "Findings summary is required when closing investigation "
                            + investigation.getInvestigationRef()
                            + ". Please provide the compliance officer's rationale before closing.");
        }
    }

    // Private helper methods

    private boolean isValidTransition(InvestigationState from, InvestigationState to) {
        Set<InvestigationState> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        return allowed.contains(to);
    }

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
        return String.format("INV%s-%05d", datePart, id);
    }

    private String buildTemporaryRef() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return "TMP" + timePart;
    }
}
