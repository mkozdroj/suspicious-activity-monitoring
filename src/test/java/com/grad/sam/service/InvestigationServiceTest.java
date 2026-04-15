package com.grad.sam.service;

import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.enums.RiskRating;
import com.grad.sam.model.*;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.CustomerRepository;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class InvestigationServiceTest {

    @Mock private InvestigationRepository investigationRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private CustomerRepository customerRepository;

    private InvestigationService service;

    private Customer customer;
    private Account account;
    private Alert alert;

    @BeforeEach
    void setUp() {
        service = new InvestigationService(investigationRepository, alertRepository, customerRepository);

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setCustomerRef("CUST-001");
        customer.setFullName("Ivan Petrov");
        customer.setNationality("RU");
        customer.setCountryOfResidence("RU");
        customer.setCustomerType("INDIVIDUAL");
        customer.setRiskRating(RiskRating.HIGH);
        customer.setKycStatus("VERIFIED");
        customer.setOnboardedDate(LocalDate.now().minusYears(2));
        customer.setIsPep(true);
        customer.setIsActive(true);

        account = new Account();
        account.setAccountId(10);
        account.setAccountNumber("ACC-0010");
        account.setAccountType("CURRENT");
        account.setCurrency("USD");
        account.setStatus("ACTIVE");
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
        alert.setStatus("OPEN");
        alert.setTriggeredAt(LocalDateTime.now());
    }

    // ── openCase ─────────────────────────────────────────────────────────────

    @Test
    void openCase_creates_investigation_and_returns_it() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(investigationRepository.save(any(Investigation.class)))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        Investigation result = service.openCase(100, "officer@bank.com", "HIGH");

        assertNotNull(result);
        assertEquals(InvestigationState.OPEN, result.getState());
        verify(investigationRepository).save(any(Investigation.class));
    }

    @Test
    void openCase_assigns_officer_and_moves_alert_to_under_review() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(investigationRepository.save(any())).thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", "URGENT");

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals("UNDER_REVIEW", alertCaptor.getValue().getStatus());
        assertEquals("officer@bank.com", alertCaptor.getValue().getAssignedTo());
    }

    @Test
    void openCase_defaults_priority_to_medium_when_null() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        when(investigationRepository.save(captor.capture()))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", null);

        assertEquals("MEDIUM", captor.getValue().getPriority());
    }

    @Test
    void openCase_returns_null_when_alert_not_found() {
        when(alertRepository.findById(999)).thenReturn(Optional.empty());

        Investigation result = service.openCase(999, "officer@bank.com", "HIGH");

        assertNull(result);
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void openCase_returns_null_when_investigation_already_exists() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100))
                .thenReturn(Optional.of(buildSavedInvestigation(InvestigationState.OPEN)));

        Investigation result = service.openCase(100, "officer@bank.com", "HIGH");

        assertNull(result);
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void openCase_returns_null_when_customer_not_found() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        Investigation result = service.openCase(100, "officer@bank.com", "LOW");

        assertNull(result);
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void openCase_sets_opened_at_to_now() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        when(investigationRepository.save(captor.capture()))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", "MEDIUM");

        assertNotNull(captor.getValue().getOpenedAt());
        assertTrue(captor.getValue().getOpenedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void openCase_links_alert_and_customer_to_investigation() {
        when(alertRepository.findById(100)).thenReturn(Optional.of(alert));
        when(investigationRepository.findByAlert_AlertId(100)).thenReturn(Optional.empty());
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        when(investigationRepository.save(captor.capture()))
                .thenReturn(buildSavedInvestigation(InvestigationState.OPEN));

        service.openCase(100, "officer@bank.com", "HIGH");

        assertEquals(alert, captor.getValue().getAlert());
        assertEquals(customer, captor.getValue().getCustomer());
    }

    // ── updateCaseStatus ─────────────────────────────────────────────────────

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
        assertEquals("SAR_FILED", alertCaptor.getValue().getStatus());
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
        assertEquals("CLOSED", alertCaptor.getValue().getStatus());
    }

    @Test
    void updateCaseStatus_returns_null_when_investigation_not_found() {
        when(investigationRepository.findById(999)).thenReturn(Optional.empty());

        Investigation result = service.updateCaseStatus(999, InvestigationState.UNDER_REVIEW, null, null);

        assertNull(result);
    }

    @Test
    void updateCaseStatus_returns_null_for_invalid_transition_open_to_closed() {
        Investigation inv = buildSavedInvestigation(InvestigationState.OPEN);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        Investigation result = service.updateCaseStatus(1, InvestigationState.CLOSED,
                InvestigationOutcome.NO_ACTION, "Skipping review.");

        assertNull(result);
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void updateCaseStatus_returns_null_when_already_closed() {
        Investigation inv = buildSavedInvestigation(InvestigationState.CLOSED);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        Investigation result = service.updateCaseStatus(1, InvestigationState.OPEN, null, null);

        assertNull(result);
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void updateCaseStatus_returns_null_when_closing_without_outcome() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));

        Investigation result = service.updateCaseStatus(1, InvestigationState.CLOSED, null, "No outcome.");

        assertNull(result);
        verify(investigationRepository, never()).save(any());
    }

    @Test
    void updateCaseStatus_stamps_closedAt_when_closing() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investigation result = service.updateCaseStatus(1, InvestigationState.CLOSED,
                InvestigationOutcome.MONITORING, null);

        assertNotNull(result.getClosedAt());
    }

    @Test
    void updateCaseStatus_does_not_overwrite_findings_with_blank() {
        Investigation inv = buildSavedInvestigation(InvestigationState.UNDER_REVIEW);
        inv.setAlert(alert);
        inv.setFindings("Existing finding note.");
        when(investigationRepository.findById(1)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Investigation result = service.updateCaseStatus(1, InvestigationState.CLOSED,
                InvestigationOutcome.NO_ACTION, "  ");

        assertEquals("Existing finding note.", result.getFindings());
    }

    // ── All valid outcomes are handled on close ───────────────────────────────

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

    // ── Query helpers ─────────────────────────────────────────────────────────

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
    void findByRef_returns_null_when_not_found() {
        when(investigationRepository.findByInvestigationRef("INV-UNKNOWN"))
                .thenReturn(Optional.empty());

        Investigation result = service.findByRef("INV-UNKNOWN");

        assertNull(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Investigation buildSavedInvestigation(InvestigationState state) {
        Investigation inv = new Investigation();
        inv.setInvestigationId(1);
        inv.setInvestigationRef("INV-260414-00001");
        inv.setAlert(alert);
        inv.setCustomer(customer);
        inv.setOpenedBy("officer@bank.com");
        inv.setOpenedAt(LocalDateTime.now());
        inv.setPriority("HIGH");
        inv.setState(state);
        return inv;
    }
}
