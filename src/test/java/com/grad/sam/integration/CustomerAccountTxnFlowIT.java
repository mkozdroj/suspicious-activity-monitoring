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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


//Integration test: Customer → Account → Transaction → Alert → AlertRule
// Uses @DataJpaTest which spins up an in-memory H2 database,
// loads all @Entity classes, and wires up all repositories.
// No Spring Boot full context needed — fast and focused on the DB layer.

@DataJpaTest
@ActiveProfiles("test")
class CustomerAccountTxnFlowIT {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TxnRepository txnRepository;
    @Autowired private AlertRuleRepository alertRuleRepository;
    @Autowired private AlertRepository alertRepository;

    private Customer customer;
    private Account account;
    private AlertRule alertRule;

    @BeforeEach
    void setUp() {
        // 1. Create and persist a customer
        customer = new Customer();
        customer.setCustomerRef("CUST-001");
        customer.setFullName("Jan Kowalski");
        customer.setDateOfBirth(LocalDate.of(1985, 3, 15));
        customer.setNationality("PL");
        customer.setCountryOfResidence("PL");
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setRiskRating(RiskRating.MEDIUM);
        customer.setKycStatus(KycStatus.VERIFIED);
        customer.setOnboardedDate(LocalDate.now().minusYears(2));
        customer.setIsPep(false);
        customer.setIsActive(true);
        customer = customerRepository.save(customer);

        // 2. Create an account linked to the customer
        account = new Account();
        account.setAccountNumber("ACC-PL-00001");
        account.setAccountType(AccountType.CURRENT);
        account.setCurrency("PLN");
        account.setBalance(new BigDecimal("5000.00"));
        account.setOpenedDate(LocalDate.now().minusYears(1));
        account.setStatus(AccountStatus.ACTIVE);
        account.setBranchCode("WAW-01");
        account.setCustomer(customer);
        account = accountRepository.save(account);

        // 3. Create an alert rule (VELOCITY — fires on large transactions)
        alertRule = new AlertRule();
        alertRule.setRuleCode("VEL-001");
        alertRule.setRuleName("Large Transaction Threshold");
        alertRule.setRuleCategory(RuleCategory.VELOCITY);
        alertRule.setDescription("Flags transactions above $10,000 USD");
        alertRule.setThresholdAmount(new BigDecimal("10000.00"));
        alertRule.setLookbackDays(30);
        alertRule.setSeverity(AlertSeverity.HIGH);
        alertRule.setIsActive(true);
        alertRule = alertRuleRepository.save(alertRule);
    }

    // Customer tests
    @Test
    void customer_is_persisted_and_retrievable_by_ref() {
        Optional<Customer> found = customerRepository.findByCustomerRef("CUST-001");

        assertTrue(found.isPresent());
        assertEquals("Jan Kowalski", found.get().getFullName());
        assertEquals(RiskRating.MEDIUM, found.get().getRiskRating());
        assertEquals("VERIFIED", found.get().getKycStatus());
    }

    @Test
    void customer_risk_rating_filter_returns_correct_results() {
        // Add a HIGH risk customer
        Customer highRisk = new Customer();
        highRisk.setCustomerRef("CUST-002");
        highRisk.setFullName("Anna Nowak");
        highRisk.setNationality("UA");
        highRisk.setCountryOfResidence("PL");
        highRisk.setCustomerType(CustomerType.INDIVIDUAL);
        highRisk.setRiskRating(RiskRating.HIGH);
        highRisk.setKycStatus(KycStatus.PENDING);
        highRisk.setOnboardedDate(LocalDate.now());
        highRisk.setIsPep(true);
        highRisk.setIsActive(true);
        customerRepository.save(highRisk);

        List<Customer> highRiskCustomers = customerRepository.findByRiskRating(RiskRating.HIGH);
        List<Customer> mediumRiskCustomers = customerRepository.findByRiskRating(RiskRating.MEDIUM);

        assertEquals(1, highRiskCustomers.size());
        assertEquals("CUST-002", highRiskCustomers.get(0).getCustomerRef());
        assertEquals(1, mediumRiskCustomers.size());
        assertEquals("CUST-001", mediumRiskCustomers.get(0).getCustomerRef());
    }

    // Account tests

    @Test
    void account_is_linked_to_customer_and_retrievable() {
        List<Account> accounts = accountRepository.findByCustomer_CustomerId(customer.getCustomerId());

        assertEquals(1, accounts.size());
        assertEquals("ACC-PL-00001", accounts.get(0).getAccountNumber());
        assertEquals("ACTIVE", accounts.get(0).getStatus());
    }

    @Test
    void account_lookup_by_account_number() {
        Optional<Account> found = accountRepository.findByAccountNumber("ACC-PL-00001");

        assertTrue(found.isPresent());
        assertEquals(customer.getCustomerId(), found.get().getCustomer().getCustomerId());
    }

    // Transaction tests
    @Test
    void transaction_is_saved_and_linked_to_account() {
        Txn txn = buildTxn("TXN-001", new BigDecimal("12000.00"), "IR");
        txn = txnRepository.save(txn);

        List<Txn> txns = txnRepository.findByAccount_AccountId(account.getAccountId());

        assertEquals(1, txns.size());
        assertEquals("TXN-001", txns.get(0).getTxnRef());
        assertEquals(0, new BigDecimal("12000.00").compareTo(txns.get(0).getAmountUsd()));
    }

    @Test
    void large_transaction_is_flagged_by_repository_query() {
        txnRepository.save(buildTxn("TXN-BIG", new BigDecimal("50000.00"), "US"));
        txnRepository.save(buildTxn("TXN-SMALL", new BigDecimal("200.00"), "PL"));

        List<Txn> large = txnRepository.findByAmountUsdGreaterThanEqualAndStatus(
                new BigDecimal("10000.00"), TxnStatus.COMPLETED);

        assertEquals(1, large.size());
        assertEquals("TXN-BIG", large.get(0).getTxnRef());
    }

    @Test
    void transactions_in_date_range_are_returned_correctly() {
        Txn old = buildTxn("TXN-OLD", new BigDecimal("5000.00"), "PL");
        old.setTxnDate(LocalDate.now().minusDays(60));
        old.setValueDate(LocalDate.now().minusDays(60));
        txnRepository.save(old);

        Txn recent = buildTxn("TXN-RECENT", new BigDecimal("3000.00"), "PL");
        txnRepository.save(recent);

        List<Txn> lastMonth = txnRepository.findByAccount_AccountIdAndTxnDateBetween(
                account.getAccountId(),
                LocalDate.now().minusDays(30),
                LocalDate.now());

        assertEquals(1, lastMonth.size());
        assertEquals("TXN-RECENT", lastMonth.get(0).getTxnRef());
    }

    // AlertRule → Alert flow
    @Test
    void alert_rule_is_persisted_and_active_rules_can_be_queried() {
        // Add an inactive rule
        AlertRule inactive = new AlertRule();
        inactive.setRuleCode("STR-001");
        inactive.setRuleName("High Velocity");
        inactive.setRuleCategory(RuleCategory.STRUCTURING);
        inactive.setDescription("Too many transactions in short window");
        inactive.setThresholdCount(10);
        inactive.setLookbackDays(7);
        inactive.setSeverity(AlertSeverity.MEDIUM);
        inactive.setIsActive(false);
        alertRuleRepository.save(inactive);

        List<AlertRule> activeRules = alertRuleRepository.findByIsActiveTrue();

        assertEquals(1, activeRules.size());
        assertEquals("THR-001", activeRules.get(0).getRuleCode());
    }

    @Test
    void alert_is_raised_against_account_and_transaction_and_rule() {
        Txn txn = txnRepository.save(buildTxn("TXN-ALERT", new BigDecimal("15000.00"), "RU"));

        Alert alert = new Alert();
        alert.setAlertRef("ALT-00001");
        alert.setAlertRule(alertRule);
        alert.setAccount(account);
        alert.setTxn(txn);
        alert.setTriggeredAt(java.time.LocalDateTime.now());
        alert.setAlertScore((short) 80);
        alert.setStatus(AlertStatus.OPEN);
        alert.setNotes("Transaction above $10,000 threshold to high-risk country");
        alert = alertRepository.save(alert);

        // Verify alert persisted correctly
        Optional<Alert> found = alertRepository.findByAlertRef("ALT-00001");
        assertTrue(found.isPresent());
        assertEquals(AlertStatus.OPEN, found.get().getStatus());
        assertEquals((short) 80, found.get().getAlertScore());
        assertEquals(account.getAccountId(), found.get().getAccount().getAccountId());
        assertEquals(txn.getTxnId(), found.get().getTxn().getTxnId());
    }

    @Test
    void open_alerts_for_account_can_be_queried() {
        Txn txn = txnRepository.save(buildTxn("TXN-002", new BigDecimal("20000.00"), "KP"));


        Alert alert1 = buildAlert("ALT-00002", txn, AlertStatus.OPEN, (short) 75);
        Alert alert2 = buildAlert("ALT-00003", txn, AlertStatus.CLOSED, (short) 40);
        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<Alert> openAlerts = alertRepository.findByStatus(AlertStatus.OPEN);
        List<Alert> closedAlerts = alertRepository.findByStatus(AlertStatus.CLOSED);

        assertEquals(1, openAlerts.size());
        assertEquals("ALT-00002", openAlerts.get(0).getAlertRef());
        assertEquals(1, closedAlerts.size());
    }

    @Test
    void high_score_alerts_are_returned_by_min_score_query() {
        Txn txn = txnRepository.save(buildTxn("TXN-003", new BigDecimal("30000.00"), "SY"));

        alertRepository.save(buildAlert("ALT-LOW",  txn, AlertStatus.OPEN, (short) 30));
        alertRepository.save(buildAlert("ALT-HIGH", txn, AlertStatus.OPEN, (short) 90));

        List<Alert> highPriority = alertRepository.findByAlertScoreGreaterThanEqual((short) 70);

        assertEquals(1, highPriority.size());
        assertEquals("ALT-HIGH", highPriority.get(0).getAlertRef());
    }

    // Helper methods
    private Txn buildTxn(String ref, BigDecimal amountUsd, String counterpartyCountry) {
        Txn txn = new Txn();
        txn.setTxnRef(ref);
        txn.setTxnType(TxnType.WIRE);
        txn.setDirection(TxnDirection.DR);
        txn.setAmount(amountUsd);
        txn.setCurrency("USD");
        txn.setAmountUsd(amountUsd);
        txn.setTxnDate(LocalDate.now());
        txn.setValueDate(LocalDate.now());
        txn.setStatus(TxnStatus.COMPLETED);
        txn.setCounterpartyCountry(counterpartyCountry);
        txn.setAccount(account);
        return txn;
    }

    private Alert buildAlert(String ref, Txn txn, AlertStatus status, short score) {
        Alert alert = new Alert();
        alert.setAlertRef(ref);
        alert.setAlertRule(alertRule);
        alert.setAccount(account);
        alert.setTxn(txn);
        alert.setTriggeredAt(java.time.LocalDateTime.now());
        alert.setAlertScore(score);
        alert.setStatus(status);
        return alert;
    }
}
