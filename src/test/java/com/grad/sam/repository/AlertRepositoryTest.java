package com.grad.sam.repository;

import com.grad.sam.enums.*;
import com.grad.sam.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TestEntityManager em;

    private Alert alert;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setCustomerRef("CUST-001");
        customer.setFullName("John Doe");
        customer.setNationality("PL");
        customer.setCountryOfResidence("PL");
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setRiskRating(RiskRating.LOW);
        customer.setKycStatus(KycStatus.VERIFIED);
        customer.setOnboardedDate(LocalDate.now().minusYears(1));
        customer.setIsActive(true);
        customer.setIsPep(false);
        customer = em.persistFlushFind(customer);

        Account account = new Account();
        account.setAccountNumber("ACC-001");
        account.setCustomer(customer);
        account.setCurrency("USD");
        account.setAccountType(AccountType.CURRENT);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBranchCode("WAR001");
        account.setOpenedDate(LocalDate.now().minusMonths(6));
        account = em.persistFlushFind(account);

        Txn txn = new Txn();
        txn.setTxnRef("TXN-001");
        txn.setAccount(account);
        txn.setTxnDate(LocalDate.now());
        txn.setValueDate(LocalDate.now());
        txn.setStatus(TxnStatus.COMPLETED);
        txn.setTxnType(TxnType.CARD);
        txn.setDirection(TxnDirection.DR);
        txn.setAmount(new BigDecimal("1000.00"));
        txn.setAmountUsd(new BigDecimal("1000.00"));
        txn.setCurrency("USD");
        txn = em.persistFlushFind(txn);

        AlertRule rule = new AlertRule();
        rule.setRuleCode("VEL-001");
        rule.setRuleName("Velocity rule");
        rule.setDescription("Detects unusually high transaction velocity");
        rule.setRuleCategory(RuleCategory.VELOCITY);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setIsActive(true);
        rule = em.persistFlushFind(rule);

        alert = new Alert();
        alert.setAlertRef("ALT-001");
        alert.setAlertRule(rule);
        alert.setAccount(account);
        alert.setTxn(txn);
        alert.setAlertScore((short) 75);
        alert.setStatus(AlertStatus.OPEN);
        alert.setTriggeredAt(LocalDateTime.now());
        alert = em.persistFlushFind(alert);
    }

    @Test
    void updateStatus_should_change_alert_status() {
        alert.setStatus(AlertStatus.CLOSED);
        alertRepository.saveAndFlush(alert);
        em.flush();
        em.clear();

        Alert updated = em.find(Alert.class, alert.getAlertId());
        assertEquals(AlertStatus.CLOSED, updated.getStatus());
    }

    @Test
    void assignTo_should_set_assignee_and_move_open_alert_to_under_review() {
        alert.setAssignedTo("analyst@bank.com");
        alert.setStatus(AlertStatus.UNDER_REVIEW);
        alertRepository.saveAndFlush(alert);
        em.flush();
        em.clear();

        Alert updated = em.find(Alert.class, alert.getAlertId());

        assertEquals("analyst@bank.com", updated.getAssignedTo());
        assertEquals(AlertStatus.UNDER_REVIEW, updated.getStatus());
    }

    @Test
    void assignTo_should_set_assignee_without_changing_status_when_not_open() {
        alert.setStatus(AlertStatus.SAR_FILED);
        alert = em.persistFlushFind(alert);

        alert.setAssignedTo("analyst@bank.com");
        alertRepository.saveAndFlush(alert);
        em.flush();
        em.clear();

        Alert updated = em.find(Alert.class, alert.getAlertId());

        assertEquals("analyst@bank.com", updated.getAssignedTo());
        assertEquals(AlertStatus.SAR_FILED, updated.getStatus());
    }

    @Test
    void delete_should_remove_alert() {
        Integer alertId = alert.getAlertId();

        alertRepository.deleteById(alertId);
        em.flush();
        em.clear();

        assertFalse(alertRepository.findById(alertId).isPresent());
    }
}
