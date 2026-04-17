package com.grad.sam.service;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RuleEngineServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private TxnRepository txnRepository;
    @Mock private DataSource dataSource;
    @Mock private WatchlistScreeningService watchlistScreeningService;

    @Mock private Connection raiseAlertConnection;
    @Mock private Connection screenTxnConnection;

    @Mock private CallableStatement raiseAlertStatement;
    @Mock private CallableStatement screenTxnStatement;

    private RuleEngineService service;
    private Account account;
    private Txn currentTxn;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-TEST");

        currentTxn = new Txn();
        currentTxn.setTxnId(42);
        currentTxn.setTxnRef("TXN-TEST-001");
        currentTxn.setAmountUsd(new BigDecimal("15000.00"));
        currentTxn.setAmount(new BigDecimal("15000.00"));
        currentTxn.setCurrency("USD");
        currentTxn.setStatus(TxnStatus.COMPLETED);
    }

    @Test
    void returns_alert_id_when_rule_fires() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule activeRule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        AmlRule mockImpl = mockRuleThatFires(RuleCategory.VELOCITY, activeRule, "Large txn detected");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(99L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(mockImpl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertEquals(1, result.size());
        assertEquals(99L, result.getFirst());
    }

    @Test
    void raises_alert_via_raise_alert_stored_proc() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule activeRule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        AmlRule mockImpl = mockRuleThatFires(RuleCategory.VELOCITY, activeRule, "Large txn");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(99L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(mockImpl), watchlistScreeningService);

        service.screenTransaction(currentTxn, account);

        verify(raiseAlertConnection).prepareCall("{CALL raise_alert(?, ?, ?, ?)}");
        verify(raiseAlertStatement).setInt(1, 42);
        verify(raiseAlertStatement).setInt(2, 1);
        verify(raiseAlertStatement).setString(3, "Large txn");
        verify(raiseAlertStatement).registerOutParameter(4, Types.BIGINT);
        verify(raiseAlertStatement).execute();
    }

    @Test
    void calls_screen_transaction_proc_after_evaluation() throws Exception {
        stubScreenTransactionOnly();

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(), watchlistScreeningService);

        service.screenTransaction(currentTxn, account);

        verify(screenTxnConnection).prepareCall("{CALL screen_transaction(?)}");
        verify(screenTxnStatement).setInt(1, 42);
        verify(screenTxnStatement).execute();
    }

    @Test
    void returns_empty_list_when_no_active_rules() throws Exception {
        stubScreenTransactionOnly();

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty());
    }

    @Test
    void skips_rule_with_no_matching_implementation() throws Exception {
        stubScreenTransactionOnly();

        AlertRule unknownCategoryRule = buildAlertRule(2, "SMU-001", RuleCategory.SMURFING, 30);
        AmlRule velocityOnlyImpl = mockRuleImplCategoryOnly(RuleCategory.VELOCITY);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(unknownCategoryRule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(velocityOnlyImpl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty());
    }

    @Test
    void skips_rule_that_throws_exception_and_continues() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule badRule = buildAlertRule(3, "BAD-001", RuleCategory.STRUCTURING, 30);
        AlertRule goodRule = buildAlertRule(4, "GEO-001", RuleCategory.GEOGRAPHY, 30);

        AmlRule faultyImpl = mock(AmlRule.class);
        when(faultyImpl.getSupportedCategory()).thenReturn(RuleCategory.STRUCTURING.name());
        when(faultyImpl.evaluate(any(RuleContext.class), eq(badRule)))
                .thenThrow(new RuntimeException("Simulated rule crash"));

        AmlRule goodImpl = mockRuleThatFires(RuleCategory.GEOGRAPHY, goodRule, "High-risk country");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(badRule, goodRule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(55L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(faultyImpl, goodImpl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertEquals(1, result.size());
        assertEquals(55L, result.getFirst());
    }

    @Test
    void returns_empty_list_when_raise_alert_proc_fails() throws Exception {
        AlertRule rule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        AmlRule impl = mockRuleThatFires(RuleCategory.VELOCITY, rule, "Large txn");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB failure"));

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty());
    }

    @Test
    void skips_rule_when_category_is_null() throws Exception {
        stubScreenTransactionOnly();

        AlertRule rule = buildAlertRule(1, "NO-CAT", RuleCategory.VELOCITY, 30);
        rule.setRuleCategory(null);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty());
    }

    @Test
    void uses_max_lookback_from_active_rules() throws Exception {
        stubScreenTransactionOnly();

        AlertRule rule1 = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 7);
        AlertRule rule2 = buildAlertRule(2, "GEO-001", RuleCategory.GEOGRAPHY, 45);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule1, rule2));
        when(txnRepository.findRecentByAccount(1, 42, 45)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(), watchlistScreeningService);

        service.screenTransaction(currentTxn, account);

        verify(txnRepository).findRecentByAccount(1, 42, 45);
    }

    @Test
    void uses_default_lookback_when_rule_lookback_is_null() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule rule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        rule.setLookbackDays(null);

        AmlRule impl = mockRuleThatFires(RuleCategory.VELOCITY, rule, "Large txn");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(99L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        service.screenTransaction(currentTxn, account);

        verify(txnRepository).findRecentByAccount(1, 42, 30);
    }

    @Test
    void returns_multiple_alert_ids_when_multiple_rules_fire() throws Exception {
        stubTwoRaiseAlertsAndScreenTransaction();

        AlertRule rule1 = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        AlertRule rule2 = buildAlertRule(2, "GEO-001", RuleCategory.GEOGRAPHY, 30);

        AmlRule impl1 = mockRuleThatFires(RuleCategory.VELOCITY, rule1, "Large txn");
        AmlRule impl2 = mockRuleThatFires(RuleCategory.GEOGRAPHY, rule2, "High-risk country");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule1, rule2));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(101L, 102L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl1, impl2), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertEquals(2, result.size());
        assertEquals(List.of(101L, 102L), result);
    }

    @Test
    void returns_empty_list_when_rule_does_not_fire() throws Exception {
        stubScreenTransactionOnly();

        AlertRule rule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);

        AmlRule impl = mock(AmlRule.class);
        when(impl.getSupportedCategory()).thenReturn(RuleCategory.VELOCITY.name());
        when(impl.evaluate(any(RuleContext.class), eq(rule))).thenReturn(Optional.empty());

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty());
        verify(raiseAlertConnection, never()).prepareCall(anyString());
    }

    @Test
    void excludes_negative_alert_id_from_results() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule rule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        AmlRule impl = mockRuleThatFires(RuleCategory.VELOCITY, rule, "Duplicate alert");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(-1L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty());
    }

    @Test
    void truncates_notes_longer_than_500_chars() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule rule = buildAlertRule(1, "VEL-001", RuleCategory.VELOCITY, 30);
        String longReason = "x".repeat(600);

        AmlRule impl = mockRuleThatFires(RuleCategory.VELOCITY, rule, longReason);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(88L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        service.screenTransaction(currentTxn, account);

        verify(raiseAlertStatement).setString(eq(3), argThat(note ->
                note != null && note.length() == 500 && note.endsWith("...")
        ));
    }

    // ── Parameterised AML category tests ─────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = RuleCategory.class, names = {"STRUCTURING", "SMURFING", "VELOCITY", "GEOGRAPHY", "WATCHLIST", "PATTERN"})
    void fires_alert_for_each_aml_rule_category(RuleCategory category) throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        AlertRule rule = buildAlertRule(10, category.name() + "-001", category, 30);
        AmlRule impl = mockRuleThatFires(category, rule, "AML hit: " + category.name());

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(200L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertEquals(1, result.size(), "Expected alert for category: " + category);
        assertEquals(200L, result.getFirst());
    }

    // ── Inactive rule skipped ─────────────────────────────────────────────────

    @Test
    void inactive_rule_is_never_evaluated() throws Exception {
        stubScreenTransactionOnly();

        AlertRule inactiveRule = buildAlertRule(99, "STR-INACTIVE", RuleCategory.STRUCTURING, 30);
        inactiveRule.setIsActive(false);

        AmlRule impl = mock(AmlRule.class);
        when(impl.getSupportedCategory()).thenReturn(RuleCategory.STRUCTURING.name());

        // findByIsActiveTrue returns empty — inactive rule was filtered by the repository
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertTrue(result.isEmpty(), "Inactive rule should not generate any alert");
        verify(impl, never()).evaluate(any(), any());
    }

    // PEP / Watchlist hit scenario

    @Test
    void pep_watchlist_hit_raises_alert_with_correct_rule_and_reason() throws Exception {
        stubOneRaiseAlertAndScreenTransaction();

        // Simulate a transaction by a PEP customer hitting a watchlist entry
        currentTxn.setCounterpartyCountry("KP"); // North Korea — sanctioned
        currentTxn.setAmountUsd(new BigDecimal("75000.00"));

        AlertRule watchlistRule = buildAlertRule(20, "WL-PEP-001", RuleCategory.WATCHLIST, 30);
        watchlistRule.setSeverity(AlertSeverity.CRITICAL);

        String expectedReason = "PEP customer matched OFAC watchlist entry — counterparty in KP";
        AmlRule watchlistImpl = mockRuleThatFires(RuleCategory.WATCHLIST, watchlistRule, expectedReason);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(watchlistRule));
        when(txnRepository.findRecentByAccount(1, 42, 30)).thenReturn(List.of());
        when(raiseAlertStatement.getLong(4)).thenReturn(300L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(watchlistImpl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(currentTxn, account);

        assertEquals(1, result.size());
        assertEquals(300L, result.getFirst());

        // Verify the correct reason was passed to raise_alert stored procedure
        verify(raiseAlertStatement).setString(3, expectedReason);
        verify(raiseAlertStatement).setInt(2, 20); // correct rule ID
    }

    private void stubScreenTransactionOnly() throws Exception {
        when(dataSource.getConnection()).thenReturn(screenTxnConnection);
        when(screenTxnConnection.prepareCall("{CALL screen_transaction(?)}"))
                .thenReturn(screenTxnStatement);
    }

    private void stubOneRaiseAlertAndScreenTransaction() throws Exception {
        when(dataSource.getConnection()).thenReturn(
                raiseAlertConnection,
                screenTxnConnection
        );

        when(raiseAlertConnection.prepareCall("{CALL raise_alert(?, ?, ?, ?)}"))
                .thenReturn(raiseAlertStatement);

        when(screenTxnConnection.prepareCall("{CALL screen_transaction(?)}"))
                .thenReturn(screenTxnStatement);
    }

    private void stubTwoRaiseAlertsAndScreenTransaction() throws Exception {
        when(dataSource.getConnection()).thenReturn(
                raiseAlertConnection,
                raiseAlertConnection,
                screenTxnConnection
        );

        when(raiseAlertConnection.prepareCall("{CALL raise_alert(?, ?, ?, ?)}"))
                .thenReturn(raiseAlertStatement);

        when(screenTxnConnection.prepareCall("{CALL screen_transaction(?)}"))
                .thenReturn(screenTxnStatement);
    }

    private AlertRule buildAlertRule(int id, String code, RuleCategory category, int lookback) {
        AlertRule r = new AlertRule();
        r.setRuleId(id);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setRuleCategory(category);
        r.setDescription("Test rule");
        r.setThresholdAmount(new BigDecimal("10000.00"));
        r.setThresholdCount(10);
        r.setLookbackDays(lookback);
        r.setSeverity(AlertSeverity.HIGH);
        r.setIsActive(true);
        return r;
    }

    private AmlRule mockRuleThatFires(RuleCategory category, AlertRule matchedRule, String reason) {
        AmlRule impl = mock(AmlRule.class);
        when(impl.getSupportedCategory()).thenReturn(category.name());
        when(impl.evaluate(any(RuleContext.class), eq(matchedRule)))
                .thenReturn(Optional.of(new RuleMatch(matchedRule, reason)));
        return impl;
    }

    private AmlRule mockRuleImplCategoryOnly(RuleCategory category) {
        AmlRule impl = mock(AmlRule.class);
        when(impl.getSupportedCategory()).thenReturn(category.name());
        return impl;
    }
}

