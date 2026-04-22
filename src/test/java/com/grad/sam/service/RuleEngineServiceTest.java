package com.grad.sam.service;

import com.grad.sam.enums.*;
import com.grad.sam.model.*;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Mock private TxnService txnService;
    @Mock private AmlRule amlRule;
    @Mock private WatchlistScreeningService watchlistScreeningService;
    @Mock private AlertService alertService;

    private RuleEngineService service;

    private Txn txn;
    private Account account;
    private Customer customer;
    private AlertRule activeRule;

    @BeforeEach
    void setUp() {
        service = new RuleEngineService(
                alertRuleRepository,
                txnRepository,
                txnService,
                List.of(amlRule),
                watchlistScreeningService,
                alertService
        );

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setFullName("Ivan Petrov");

        account = new Account();
        account.setAccountId(10);
        account.setAccountNumber("ACC-0010");
        account.setCurrency("USD");
        account.setCustomer(customer);

        txn = new Txn();
        txn.setTxnId(42);
        txn.setTxnRef("TXN-TEST-001");
        txn.setAmountUsd(new BigDecimal("15000.00"));
        txn.setAccount(account);

        activeRule = buildRule(1, "STR-001", RuleCategory.STRUCTURING, 30);
        stubSupports(amlRule, "STR-");
    }


    // screenTransaction — happy paths

    @Test
    void screenTransaction_returns_alert_id_when_single_rule_fires() {
        Alert alert = buildAlert(1, "ALT-001");
        RuleMatch match = new RuleMatch(activeRule, "Structuring pattern detected");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(eq(10), eq(42), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(RuleContext.class), eq(activeRule))).thenReturn(Optional.of(match));
        when(alertService.createAlert(txn, account, activeRule, "Structuring pattern detected")).thenReturn(alert);

        List<Long> result = service.screenTransaction(txn, account);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
    }

    @Test
    void screenTransaction_returns_multiple_alert_ids_when_multiple_rules_fire() {
        AlertRule secondRule = buildRule(2, "VEL-001", RuleCategory.VELOCITY, 7);
        AmlRule velocityImpl = mock(AmlRule.class);
        stubSupports(velocityImpl, "VEL-");

        service = new RuleEngineService(
                alertRuleRepository, txnRepository, txnService,
                List.of(amlRule, velocityImpl),
                watchlistScreeningService, alertService
        );

        RuleMatch match1 = new RuleMatch(activeRule, "Structuring pattern detected");
        RuleMatch match2 = new RuleMatch(secondRule, "High velocity");

        Alert alert1 = buildAlert(1, "ALT-001");
        Alert alert2 = buildAlert(2, "ALT-002");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule, secondRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(), eq(activeRule))).thenReturn(Optional.of(match1));
        when(velocityImpl.evaluate(any(), eq(secondRule))).thenReturn(Optional.of(match2));
        when(alertService.createAlert(txn, account, activeRule, "Structuring pattern detected")).thenReturn(alert1);
        when(alertService.createAlert(txn, account, secondRule, "High velocity")).thenReturn(alert2);

        List<Long> result = service.screenTransaction(txn, account);

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(1L, 2L)));
    }

    @Test
    void screenTransaction_returns_empty_list_when_no_active_rules() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
        verify(alertService, never()).createAlert(any(), any(), any(), any());
    }

    @Test
    void screenTransaction_returns_empty_list_when_no_rules_match() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(RuleContext.class), eq(activeRule))).thenReturn(Optional.empty());

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
        verify(alertService, never()).createAlert(any(), any(), any(), any());
    }

    // screenTransaction — transaction status

    @Test
    void screenTransaction_marks_txn_as_screened_when_rules_fire() {
        RuleMatch match = new RuleMatch(activeRule, "Structuring pattern detected");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(), eq(activeRule))).thenReturn(Optional.of(match));
        when(alertService.createAlert(any(), any(), any(), any())).thenReturn(buildAlert(1, "ALT-001"));

        service.screenTransaction(txn, account);

        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
    }

    @Test
    void screenTransaction_marks_txn_as_screened_even_when_no_rules_fire() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(), eq(activeRule))).thenReturn(Optional.empty());

        service.screenTransaction(txn, account);

        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
    }

    @Test
    void screenTransaction_marks_txn_as_screened_even_when_no_active_rules() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        service.screenTransaction(txn, account);

        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
    }

    // screenTransaction — watchlist screening

    @Test
    void screenTransaction_calls_watchlist_screening_with_customer_name_and_fuzzy_score() throws Exception {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        service.screenTransaction(txn, account);

        verify(watchlistScreeningService).screenCustomer(
                "Ivan Petrov",
                WatchlistScreeningService.FUZZY_MATCH_SCORE,
                txn
        );
    }

    @Test
    void screenTransaction_continues_and_returns_alerts_when_watchlist_screening_throws() throws Exception {
        RuleMatch match = new RuleMatch(activeRule, "Structuring pattern detected");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(), eq(activeRule))).thenReturn(Optional.of(match));
        when(alertService.createAlert(any(), any(), any(), any())).thenReturn(buildAlert(5, "ALT-005"));
        doThrow(new RuntimeException("Watchlist service unavailable"))
                .when(watchlistScreeningService).screenCustomer(any(), any(), any());

        List<Long> result = service.screenTransaction(txn, account);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0));
    }

    @Test
    void screenTransaction_skips_rule_when_no_implementation_supports_rule_code() {
        AlertRule unsupportedRule = buildRule(4, "ZZZ-001", RuleCategory.STRUCTURING, 30);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(unsupportedRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
        verify(alertService, never()).createAlert(any(), any(), any(), any());
    }

    @Test
    void screenTransaction_throws_when_active_rules_query_returns_null() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.screenTransaction(txn, account));
    }

    @Test
    void screenTransaction_throws_when_recent_transactions_query_returns_null() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.screenTransaction(txn, account));
    }

    @Test
    void screenTransaction_continues_when_customer_name_is_missing_for_watchlist_screening() {
        customer.setFullName("   ");
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
        verify(watchlistScreeningService, never()).screenCustomer(any(), any(), any());
    }

    @Test
    void screenTransaction_throws_when_transaction_id_is_null() {
        txn.setTxnId(null);

        assertThrows(IllegalArgumentException.class, () -> service.screenTransaction(txn, account));
    }

    @Test
    void screenTransaction_throws_when_account_id_is_null() {
        account.setAccountId(null);

        assertThrows(IllegalArgumentException.class, () -> service.screenTransaction(txn, account));
    }

    // screenTransaction — lookback days

    @Test
    void screenTransaction_uses_max_lookback_days_across_all_rules() {
        AlertRule shortLookback = buildRule(2, "VEL-001", RuleCategory.VELOCITY, 7);
        AlertRule longLookback  = buildRule(3, "STR-001", RuleCategory.STRUCTURING, 90);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(shortLookback, longLookback));

        service.screenTransaction(txn, account);

        verify(txnRepository).findRecentByAccount(10, 42, 90);
    }

    @Test
    void screenTransaction_defaults_lookback_to_30_when_rule_has_null_lookback() {
        AlertRule noLookback = buildRule(2, "VEL-001", RuleCategory.VELOCITY, null);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(noLookback));

        service.screenTransaction(txn, account);

        verify(txnRepository).findRecentByAccount(10, 42, 30);
    }

    @Test
    void screenTransaction_defaults_lookback_to_30_when_no_active_rules() {
        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());

        service.screenTransaction(txn, account);

        verify(txnRepository).findRecentByAccount(10, 42, 30);
    }

    // screenTransaction — RuleContext

    @Test
    void screenTransaction_passes_correct_context_to_rule_implementation() {
        List<Txn> recentTxns = List.of(buildPreviousTxn(99));

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(10, 42, 30)).thenReturn(recentTxns);
        when(amlRule.evaluate(any(RuleContext.class), eq(activeRule))).thenReturn(Optional.empty());

        service.screenTransaction(txn, account);

        ArgumentCaptor<RuleContext> contextCaptor = ArgumentCaptor.forClass(RuleContext.class);
        verify(amlRule).evaluate(contextCaptor.capture(), eq(activeRule));

        RuleContext captured = contextCaptor.getValue();
        assertEquals(txn, captured.getTxn());
        assertEquals(account, captured.getAccount());
        assertEquals(recentTxns, captured.getRecentTxns());
    }

    // evaluateRule — resilience (tested via screenTransaction)

    @Test
    void screenTransaction_skips_rule_with_null_category() {
        AlertRule nullCategoryRule = buildRule(2, "NULL-001", null, 30);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(nullCategoryRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
        verify(alertService, never()).createAlert(any(), any(), any(), any());
    }

    @Test
    void screenTransaction_skips_rule_when_no_implementation_registered_for_category() {
        // GEOGRAPHY has no AmlRule implementation registered in setUp()
        AlertRule geoRule = buildRule(2, "GEO-001", RuleCategory.GEOGRAPHY, 30);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(geoRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
        verify(alertService, never()).createAlert(any(), any(), any(), any());
    }

    @Test
    void screenTransaction_skips_rule_and_continues_when_evaluation_throws() {
        AlertRule throwingRule = buildRule(2, "STR-002", RuleCategory.STRUCTURING, 30);
        AlertRule goodRule     = buildRule(3, "VEL-001", RuleCategory.VELOCITY, 30);
        AmlRule velocityImpl   = mock(AmlRule.class);
        stubSupports(velocityImpl, "VEL-");

        service = new RuleEngineService(
                alertRuleRepository, txnRepository, txnService,
                List.of(amlRule, velocityImpl),
                watchlistScreeningService, alertService
        );

        RuleMatch goodMatch = new RuleMatch(goodRule, "Velocity breach");
        Alert alert = buildAlert(7, "ALT-007");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(throwingRule, goodRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(amlRule.evaluate(any(), eq(throwingRule)))
                .thenThrow(new RuntimeException("Rule engine internal error"));
        when(velocityImpl.evaluate(any(), eq(goodRule))).thenReturn(Optional.of(goodMatch));
        when(alertService.createAlert(txn, account, goodRule, "Velocity breach")).thenReturn(alert);

        List<Long> result = service.screenTransaction(txn, account);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0));
    }

    // Constructor — duplicate category registration

    @Test
    void constructor_keeps_first_rule_when_two_rules_share_the_same_category() {
        AmlRule firstImpl  = mock(AmlRule.class);
        AmlRule secondImpl = mock(AmlRule.class);
        stubSupports(firstImpl, "STR-");
        stubSupports(secondImpl, "STR-");

        service = new RuleEngineService(
                alertRuleRepository, txnRepository, txnService,
                List.of(firstImpl, secondImpl),
                watchlistScreeningService, alertService
        );

        RuleMatch match = new RuleMatch(activeRule, "Hit");
        Alert alert = buildAlert(1, "ALT-001");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(activeRule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(firstImpl.evaluate(any(), eq(activeRule))).thenReturn(Optional.of(match));
        when(alertService.createAlert(any(), any(), any(), any())).thenReturn(alert);

        service.screenTransaction(txn, account);

        verify(firstImpl).evaluate(any(), eq(activeRule));
        verify(secondImpl, never()).evaluate(any(), any());
    }

    // Helpers

    private AlertRule buildRule(int id, String code, RuleCategory category, Integer lookbackDays) {
        AlertRule rule = new AlertRule();
        rule.setRuleId(id);
        rule.setRuleCode(code);
        rule.setRuleName(code + " Rule");
        rule.setRuleCategory(category);
        rule.setDescription("Test rule: " + code);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setLookbackDays(lookbackDays);
        rule.setIsActive(true);
        return rule;
    }

    private Alert buildAlert(int id, String ref) {
        Alert alert = new Alert();
        alert.setAlertId(id);
        alert.setAlertRef(ref);
        alert.setAlertScore((short) 80);
        alert.setStatus(AlertStatus.OPEN);
        return alert;
    }

    private Txn buildPreviousTxn(int id) {
        Txn t = new Txn();
        t.setTxnId(id);
        t.setTxnRef("TXN-PREV-" + id);
        t.setAmountUsd(new BigDecimal("5000.00"));
        t.setTxnDate(LocalDate.now().minusDays(2));
        return t;
    }

    private void stubSupports(AmlRule rule, String prefix) {
        lenient().when(rule.supports(any(AlertRule.class))).thenAnswer(invocation -> {
            AlertRule candidate = invocation.getArgument(0);
            return candidate != null
                    && candidate.getRuleCode() != null
                    && candidate.getRuleCode().startsWith(prefix);
        });
    }
}
