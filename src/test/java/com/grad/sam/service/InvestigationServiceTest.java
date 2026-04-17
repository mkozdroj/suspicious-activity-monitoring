package com.grad.sam.service;

import com.grad.sam.enums.*;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.model.*;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.InvestigationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class InvestigationServiceTest {

    @Mock private InvestigationRepository investigationRepository;
    @Mock private AlertRepository alertRepository;

    private InvestigationService service;

    private Customer customer;
    private Account account;
    private Alert alert;

    @BeforeEach
    void setUp() {
        service = new InvestigationService(investigationRepository, alertRepository);

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setCustomerRef("CUST-001");
        customer.setFullName("Ivan Petrov");
        customer.setNationality("RU");
        customer.setCountryOfResidence("RU");
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setRiskRating(RiskRating.HIGH);
        customer.setKycStatus(KycStatus.VERIFIED);
        customer.setOnboardedDate(LocalDate.now().minusYears(2));
        customer.setIsPep(true);
        customer.setIsActive(true);

        account = new Account();
        account.setAccountId(10);
        account.setAccountNumber("ACC-0010");
        account.setAccountType(AccountType.CURRENT);
        account.setCurrency("USD");
        account.setStatus(AccountStatus.ACTIVE);
        account.setCustomer(customer);

        AlertRule alertRule = new AlertRule();
        alertRule.setRuleId(1);
        alertRule.setRuleCode("GEO-001");

        alert = new Alert();
        alert.setAlertId(100);
        alert.setAlertRef("ALT-00100");
        alert.setAlertRule(alertRule);
        alert.setAccount(account);
        alert.setAlertScore((short) 90);
        alert.setStatus(AlertStatus.OPEN);
        alert.setTriggeredAt(LocalDateTime.now());
    }

    // openCase

    @Test
    void openCase_generates_reference_and_persists_it_on_second_save() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());

        when(investigationRepository.save(any(Investigation.class)))
                .thenAnswer(invocation -> {
                    Investigation inv = invocation.getArgument(0);
                    if (inv.getInvestigationId() == null) {
                        inv.setInvestigationId(123);
                    }
                    return inv;
                });

        Investigation result = service.openCase(100, "officer@bank.com", Priority.HIGH);

        assertNotNull(result);
        assertEquals(123, result.getInvestigationId());
        assertNotNull(result.getInvestigationRef());
        assertTrue(result.getInvestigationRef().startsWith("INV-"));
        assertTrue(result.getInvestigationRef().endsWith("-00123"));

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        verify(investigationRepository, times(2)).save(captor.capture());

        assertEquals(2, captor.getAllValues().size());

        Investigation secondSave = captor.getAllValues().get(1);
        assertNotNull(secondSave.getInvestigationRef());
        assertTrue(secondSave.getInvestigationRef().startsWith("INV-"));
        assertTrue(secondSave.getInvestigationRef().endsWith("-00123"));
    }

    @Test
    void openCase_creates_investigation_and_returns_it() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(investigationRepository.save(any(Investigation.class)))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        Investigation result = service.openCase(100, "officer@bank.com", Priority.HIGH);

        assertNotNull(result);
        assertEquals(InvestigationState.OPEN, result.getState());
        verify(investigationRepository, times(2)).save(any(Investigation.class));
    }

    @Test
    void openCase_assigns_officer_and_moves_alert_to_under_review() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(investigationRepository.save(any())).thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", Priority.URGENT);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(AlertStatus.UNDER_REVIEW, alertCaptor.getValue().getStatus());
        assertEquals("officer@bank.com", alertCaptor.getValue().getAssignedTo());
    }

    @Test
    void openCase_defaults_priority_to_medium_when_null() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        when(investigationRepository.save(captor.capture()))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", null);

        assertEquals(Priority.MEDIUM, captor.getAllValues().get(0).getPriority());
    }

    @Test
    void openCase_throws_when_alert_not_found() {
        when(alertRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class,
                () -> service.openCase(999, "officer@bank.com", Priority.HIGH));
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void openCase_throws_when_investigation_already_exists() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100))
                .thenReturn(Optional.of(buildSavedInvestigation(InvestigationState.OPEN)));

        assertThrows(DataNotFoundException.class,
                () -> service.openCase(100, "officer@bank.com", Priority.HIGH));
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void openCase_throws_when_customer_is_null() {
        account.setCustomer(null);
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class,
                () -> service.openCase(100, "officer@bank.com", Priority.LOW));
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void openCase_sets_opened_at_to_now() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        when(investigationRepository.save(captor.capture()))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", Priority.MEDIUM);

        assertNotNull(captor.getAllValues().get(0).getOpenedAt());
        assertTrue(captor.getAllValues().get(0).getOpenedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void openCase_links_alert_and_customer_to_investigation() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        when(investigationRepository.save(captor.capture()))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", Priority.HIGH);

        assertEquals(alert, captor.getAllValues().get(0).getAlert());
        assertEquals(customer, captor.getAllValues().get(0).getCustomer());
    }

    @Test
    void openCase_throws_when_alert_account_is_null_current_behavior() {
        alert.setAccount(null);

        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class,
                () -> service.openCase(100, "officer@bank.com", Priority.HIGH));
    }

    // updateCaseStatus

    @Test
    void updateCaseStatus_open_to_under_review_succeeds() {
        Investigation inv = buildSavedInvestigation(InvestigationState.OPEN);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investigation result = service.updateCaseStatus(1, InvestigationState.UNDER_REVIEW, null, null);

        assertNotNull(result);
        assertEquals(InvestigationState.UNDER_REVIEW, result.getState());
    }

    @Test
    void updateCaseStatus_under_review_to_closed_succeeds() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investigation result = service.updateCaseStatus(
                1, InvestigationState.CLOSED, InvestigationOutcome.SAR_FILED, "Confirmed.");

        assertNotNull(result);
        assertEquals(InvestigationState.CLOSED, result.getState());
        assertEquals(InvestigationOutcome.SAR_FILED, result.getOutcome());
        assertNotNull(result.getClosedAt());
    }

    @Test
    void updateCaseStatus_sets_alert_to_sar_filed_when_outcome_is_sar_filed() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCaseStatus(1, InvestigationState.CLOSED, InvestigationOutcome.SAR_FILED, "SAR filed.");

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(AlertStatus.SAR_FILED, alertCaptor.getValue().getStatus());
    }

    @Test
    void updateCaseStatus_sets_alert_to_escalated_when_outcome_is_escalated() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCaseStatus(1, InvestigationState.CLOSED, InvestigationOutcome.ESCALATED, "Escalated.");

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(AlertStatus.ESCALATED, alertCaptor.getValue().getStatus());
    }

    @Test
    void updateCaseStatus_sets_alert_to_under_review_when_outcome_is_monitoring() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCaseStatus(1, InvestigationState.CLOSED, InvestigationOutcome.MONITORING, "Monitor.");

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(AlertStatus.UNDER_REVIEW, alertCaptor.getValue().getStatus());
    }

    @Test
    void updateCaseStatus_sets_alert_to_closed_when_outcome_is_no_action() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCaseStatus(1, InvestigationState.CLOSED, InvestigationOutcome.NO_ACTION, "Legitimate.");

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(AlertStatus.CLOSED, alertCaptor.getValue().getStatus());
    }

    @Test
    void updateCaseStatus_throws_when_investigation_not_found() {
        when(investigationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class,
                () -> service.updateCaseStatus(999, InvestigationState.UNDER_REVIEW, null, null));
    }

    @Test
    void updateCaseStatus_throws_for_invalid_transition_open_to_closed() {
        Investigation inv = buildSavedInvestigation(InvestigationState.OPEN);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        assertThrows(DataNotFoundException.class,
                () -> service.updateCaseStatus(1, InvestigationState.CLOSED,
                        InvestigationOutcome.NO_ACTION, "Skipping review."));
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void updateCaseStatus_throws_when_already_closed() {
        Investigation inv = buildSavedInvestigation(InvestigationState.CLOSED);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        assertThrows(DataNotFoundException.class,
                () -> service.updateCaseStatus(1, InvestigationState.OPEN, null, null));
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void updateCaseStatus_throws_when_closing_without_outcome() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        assertThrows(InvalidInputException.class,
                () -> service.updateCaseStatus(1, InvestigationState.CLOSED, null, "No outcome."));
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void updateCaseStatus_stamps_closedAt_when_closing() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investigation result = service.updateCaseStatus(1, InvestigationState.CLOSED,
                InvestigationOutcome.MONITORING, "Ongoing monitoring required.");

        assertNotNull(result.getClosedAt());
    }

    @Test
    void updateCaseStatus_throws_when_closing_with_blank_findings() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        assertThrows(InvalidInputException.class,
                () -> service.updateCaseStatus(1, InvestigationState.CLOSED,
                        InvestigationOutcome.NO_ACTION, "  "));
        verify(investigationRepository, never()).save(any());
    }

    // All valid outcomes are handled on close
    @ParameterizedTest
    @EnumSource(InvestigationOutcome.class)
    void all_outcomes_are_accepted_on_close(InvestigationOutcome outcome) {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investigation result = service.updateCaseStatus(1, InvestigationState.CLOSED,
                outcome, "Findings for " + outcome);

        assertNotNull(result);
        assertEquals(InvestigationState.CLOSED, result.getState());
        assertEquals(outcome, result.getOutcome());
    }

    // Query helpers

    @Test
    void findOpenCases_delegates_to_repository() {
        when(investigationRepository.findByState(InvestigationState.OPEN))
                .thenReturn(List.of(buildSavedInvestigation(InvestigationState.OPEN)));

        List<Investigation> result = service.findOpenCases();

        assertEquals(1, result.size());
        verify(investigationRepository).findByState(InvestigationState.OPEN);
    }

    @Test
    void findCasesUnderReview_delegates_to_repository() {
        when(investigationRepository.findByState(InvestigationState.UNDER_REVIEW))
                .thenReturn(List.of());

        List<Investigation> result = service.findCasesUnderReview();

        assertTrue(result.isEmpty());
        verify(investigationRepository).findByState(InvestigationState.UNDER_REVIEW);
    }

    @Test
    void findByOfficer_returns_investigations_for_officer() {
        when(investigationRepository.findByOpenedBy("officer@bank.com"))
                .thenReturn(List.of(buildSavedInvestigation(InvestigationState.OPEN)));

        List<Investigation> result = service.findByOfficer("officer@bank.com");

        assertEquals(1, result.size());
    }

    @Test
    void findByRef_returns_investigation_when_found() {
        when(investigationRepository.findByInvestigationRef("INV-260414-00001"))
                .thenReturn(Optional.of(buildSavedInvestigation(InvestigationState.OPEN)));

        Investigation result = service.findByRef("INV-260414-00001");

        assertNotNull(result);
        assertEquals(InvestigationState.OPEN, result.getState());
    }

    @Test
    void findByRef_throws_when_not_found() {
        when(investigationRepository.findByInvestigationRef("INV-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class,
                () -> service.findByRef("INV-UNKNOWN"));
    }

    // Helper methods
    private Investigation buildSavedInvestigation(InvestigationState state) {
        return buildSavedInvestigation(state, "officer@bank.com", Priority.MEDIUM);
    }

    private Investigation buildSavedInvestigation(InvestigationState state, String openedBy, Priority priority) {
        Investigation inv = new Investigation();
        inv.setInvestigationId(1);
        inv.setInvestigationRef("INV-001");
        inv.setState(state);
        inv.setOpenedBy(openedBy);
        inv.setPriority(priority);
        inv.setOpenedAt(LocalDateTime.now());
        return inv;
    }
}
