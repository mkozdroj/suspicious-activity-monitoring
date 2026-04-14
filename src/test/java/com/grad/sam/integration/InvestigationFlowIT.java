package com.grad.sam.integration;

import com.grad.sam.enums.*;
import com.grad.sam.model.*;
import com.grad.sam.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


// Integration test: Alert → Investigation lifecycle
// Tests the complete AML case management flow:
// an alert is raised on a suspicious transaction, an investigator
// opens a case, reviews it, and closes it with an outcome.

@DataJpaTest
@ActiveProfiles("test")
class InvestigationFlowIT {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TxnRepository txnRepository;
    @Autowired private AlertRuleRepository alertRuleRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private InvestigationRepository investigationRepository;

    private Customer customer;
    private Account account;
    private Txn txn;
    private AlertRule alertRule;
    private Alert alert;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerRef("CUST-INV-001");
        customer.setFullName("Maria Wiśniewska");
        customer.setNationality("PL");
        customer.setCountryOfResidence("PL");
        customer.setCustomerType("INDIVIDUAL");
        customer.setRiskRating(RiskRating.HIGH);
        customer.setKycStatus("VERIFIED");
        customer.setOnboardedDate(LocalDate.now().minusYears(3));
        customer.setIsPep(true);
        customer.setIsActive(true);
        customer = customerRepository.save(customer);

        account = new Account();
        account.setAccountNumber("ACC-INV-001");
        account.setAccountType("CURRENT");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("100000.00"));
        account.setOpenedDate(LocalDate.now().minusYears(2));
        account.setStatus("ACTIVE");
        account.setBranchCode("WAW-02");
        account.setCustomer(customer);
        account = accountRepository.save(account);

        txn = new Txn();
        txn.setTxnRef("TXN-INV-001");
        txn.setTxnType("WIRE");
        txn.setDirection("DR");
        txn.setAmount(new BigDecimal("75000.00"));
        txn.setCurrency("USD");
        txn.setAmountUsd(new BigDecimal("75000.00"));
        txn.setTxnDate(LocalDate.now());
        txn.setValueDate(LocalDate.now());
        txn.setStatus("COMPLETED");
        txn.setCounterpartyCountry("KP");
        txn.setDescription("Investment transfer");
        txn.setAccount(account);
        txn = txnRepository.save(txn);

        alertRule = new AlertRule();
        alertRule.setRuleCode("GEO-001");
        alertRule.setRuleName("PEP Large Transaction");
        alertRule.setRuleCategory(RuleCategory.GEOGRAPHY);
        alertRule.setDescription("Flags large transactions from PEP customers");
        alertRule.setThresholdAmount(new BigDecimal("50000.00"));
        alertRule.setLookbackDays(30);
        alertRule.setSeverity(AlertSeverity.CRITICAL);
        alertRule.setIsActive(true);
        alertRule = alertRuleRepository.save(alertRule);

        alert = new Alert();
        alert.setAlertRef("ALT-INV-001");
        alert.setAlertRule(alertRule);
        alert.setAccount(account);
        alert.setTxn(txn);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setAlertScore((short) 95);
        alert.setStatus("OPEN");
        alert.setNotes("PEP customer wired $75,000 to North Korea");
        alert = alertRepository.save(alert);
    }

    // Open investigation from alert

    @Test
    void investigation_is_opened_from_alert_and_persisted() {
        Investigation inv = new Investigation();
        inv.setInvestigationRef("INV-00001");
        inv.setAlert(alert);
        inv.setCustomer(customer);
        inv.setOpenedBy("officer@bank.com");
        inv.setOpenedAt(LocalDateTime.now());
        inv.setPriority("URGENT");
        inv.setState(InvestigationState.OPEN);
        inv = investigationRepository.save(inv);

        assertNotNull(inv.getInvestigationId());

        Optional<Investigation> found = investigationRepository.findByInvestigationRef("INV-00001");
        assertTrue(found.isPresent());
        assertEquals(InvestigationState.OPEN, found.get().getState());
        assertEquals("URGENT", found.get().getPriority());
        assertEquals(customer.getCustomerId(), found.get().getCustomer().getCustomerId());
    }

    @Test
    void investigation_is_linked_to_alert_one_to_one() {
        Investigation inv = buildInvestigation("INV-00002", InvestigationState.OPEN, null);
        investigationRepository.save(inv);

        Optional<Investigation> found = investigationRepository.findByAlert_AlertId(alert.getAlertId());
        assertTrue(found.isPresent());
        assertEquals("INV-00002", found.get().getInvestigationRef());
    }

    @Test
    void investigations_for_customer_are_queryable() {
        investigationRepository.save(buildInvestigation("INV-00003", InvestigationState.OPEN, null));

        List<Investigation> byCustomer = investigationRepository
                .findByCustomer_CustomerId(customer.getCustomerId());

        assertEquals(1, byCustomer.size());
        assertEquals("INV-00003", byCustomer.get(0).getInvestigationRef());
    }

    // State transitions
    @Test
    void investigation_transitions_from_open_to_under_review() {
        Investigation inv = investigationRepository.save(
                buildInvestigation("INV-00004", InvestigationState.OPEN, null));

        // Investigator picks up the case
        inv.setState(InvestigationState.UNDER_REVIEW);
        investigationRepository.save(inv);

        List<Investigation> underReview = investigationRepository
                .findByState(InvestigationState.UNDER_REVIEW);
        List<Investigation> open = investigationRepository
                .findByState(InvestigationState.OPEN);

        assertEquals(1, underReview.size());
        assertTrue(open.isEmpty());
    }

    @Test
    void investigation_closes_with_sar_filed_outcome() {
        Investigation inv = investigationRepository.save(
                buildInvestigation("INV-00005", InvestigationState.UNDER_REVIEW, null));

        // Close the case — SAR filed with law enforcement
        inv.setState(InvestigationState.CLOSED);
        inv.setOutcome(InvestigationOutcome.SAR_FILED);
        inv.setFindings("Customer wired $75,000 to a sanctioned entity in North Korea. SAR filed.");
        inv.setClosedAt(LocalDateTime.now());
        investigationRepository.save(inv);

        // Update the linked alert status too
        alert.setStatus("SAR_FILED");
        alertRepository.save(alert);

        Optional<Investigation> closed = investigationRepository.findByInvestigationRef("INV-00005");
        assertTrue(closed.isPresent());
        assertEquals(InvestigationState.CLOSED, closed.get().getState());
        assertEquals(InvestigationOutcome.SAR_FILED, closed.get().getOutcome());
        assertNotNull(closed.get().getClosedAt());

        assertEquals("SAR_FILED", alertRepository.findByAlertRef("ALT-INV-001")
                .map(Alert::getStatus).orElse(""));
    }

    @Test
    void investigation_closes_with_no_action_outcome() {
        Investigation inv = investigationRepository.save(
                buildInvestigation("INV-00006", InvestigationState.UNDER_REVIEW, null));

        inv.setState(InvestigationState.CLOSED);
        inv.setOutcome(InvestigationOutcome.NO_ACTION);
        inv.setFindings("Transaction verified as legitimate business payment. No further action.");
        inv.setClosedAt(LocalDateTime.now());
        investigationRepository.save(inv);

        List<Investigation> noAction = investigationRepository
                .findByOutcome(InvestigationOutcome.NO_ACTION);
        assertEquals(1, noAction.size());
    }

    // Priority and assignment
    @Test
    void urgent_investigations_can_be_filtered_by_priority() {
        investigationRepository.save(
                buildInvestigation("INV-URGENT", InvestigationState.OPEN, null));

        // Also add a medium priority one
        Alert alert2 = buildExtraAlert("ALT-INV-002");
        alert2 = alertRepository.save(alert2);

        Investigation medium = new Investigation();
        medium.setInvestigationRef("INV-MEDIUM");
        medium.setAlert(alert2);
        medium.setCustomer(customer);
        medium.setOpenedBy("officer@bank.com");
        medium.setOpenedAt(LocalDateTime.now());
        medium.setPriority("MEDIUM");
        medium.setState(InvestigationState.OPEN);
        investigationRepository.save(medium);

        List<Investigation> urgent = investigationRepository.findByPriority("URGENT");
        List<Investigation> mediumList = investigationRepository.findByPriority("MEDIUM");

        assertEquals(1, urgent.size());
        assertEquals("INV-URGENT", urgent.get(0).getInvestigationRef());
        assertEquals(1, mediumList.size());
    }

    @Test
    void open_cases_assigned_to_officer_are_queryable() {
        investigationRepository.save(
                buildInvestigation("INV-ASSIGNED", InvestigationState.OPEN, null));

        List<Investigation> byOfficer = investigationRepository.findByOpenedBy("officer@bank.com");
        assertEquals(1, byOfficer.size());
        assertEquals("INV-ASSIGNED", byOfficer.get(0).getInvestigationRef());
    }

    // Helper methods
    private Investigation buildInvestigation(String ref, InvestigationState state,
                                              InvestigationOutcome outcome) {
        Investigation inv = new Investigation();
        inv.setInvestigationRef(ref);
        inv.setAlert(alert);
        inv.setCustomer(customer);
        inv.setOpenedBy("officer@bank.com");
        inv.setOpenedAt(LocalDateTime.now());
        inv.setPriority("URGENT");
        inv.setState(state);
        inv.setOutcome(outcome);
        return inv;
    }

    private Alert buildExtraAlert(String ref) {
        Alert a = new Alert();
        a.setAlertRef(ref);
        a.setAlertRule(alertRule);
        a.setAccount(account);
        a.setTxn(txn);
        a.setTriggeredAt(LocalDateTime.now());
        a.setAlertScore((short) 60);
        a.setStatus("OPEN");
        return a;
    }
}
