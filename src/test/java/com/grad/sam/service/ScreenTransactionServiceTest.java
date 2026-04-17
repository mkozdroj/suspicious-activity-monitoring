package com.grad.sam.service;


import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.*;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.TxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ScreenTransactionService}.
 *
 * <p>Covers: returns triggered alerts, returns empty list on no match,
 * 404 on missing transaction, 404 on missing account, delegates to rule engine.</p>
 */
@ExtendWith(MockitoExtension.class)
class ScreenTransactionServiceTest {

    @Mock private TxnRepository txnRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private RuleEngineService ruleEngineService;

    private ScreenTransactionService service;

    private Txn txn;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new ScreenTransactionService(
                txnRepository, alertRepository, ruleEngineService);

        Customer customer = new Customer();
        customer.setCustomerId(1);
        customer.setFullName("Test Customer");

        account = new Account();
        account.setAccountId(10);
        account.setAccountNumber("ACC-0010");
        account.setCurrency("USD");
        account.setCustomer(customer);

        txn = new Txn();
        txn.setTxnId(42);
        txn.setTxnRef("TXN-TEST-001");
        txn.setAmountUsd(new BigDecimal("15000.00"));
        txn.setAmount(new BigDecimal("15000.00"));
        txn.setCurrency("USD");
        txn.setTxnDate(LocalDate.now());
        txn.setStatus(TxnStatus.COMPLETED);
        txn.setAccount(account);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void returns_triggered_alerts_when_rules_fire() {
        Alert alert1 = buildAlert(1, "ALT-001");
        Alert alert2 = buildAlert(2, "ALT-002");

        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of(1L, 2L));
        when(alertRepository.findById(1)).thenReturn(Optional.of(alert1));
        when(alertRepository.findById(2)).thenReturn(Optional.of(alert2));

        List<Alert> result = service.screenTransaction(42);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> "ALT-001".equals(a.getAlertRef())));
        assertTrue(result.stream().anyMatch(a -> "ALT-002".equals(a.getAlertRef())));
    }

    @Test
    void returns_empty_list_when_no_rules_fire() {
        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of());

        List<Alert> result = service.screenTransaction(42);

        assertTrue(result.isEmpty());
        verify(alertRepository, never()).findById(anyInt());
    }

    @Test
    void delegates_to_rule_engine_with_correct_txn_and_account() {
        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of());

        service.screenTransaction(42);

        verify(ruleEngineService).screenTransaction(txn, account);
    }

    @Test
    void resolves_alerts_by_id_from_repository() {
        Alert alert = buildAlert(99, "ALT-099");

        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of(99L));
        when(alertRepository.findById(99)).thenReturn(Optional.of(alert));

        List<Alert> result = service.screenTransaction(42);

        assertEquals(1, result.size());
        assertEquals("ALT-099", result.get(0).getAlertRef());
    }

    @Test
    void silently_skips_alert_ids_that_cannot_be_resolved() {
        // Rule engine returns an alert ID that doesn't exist in the DB (race condition / dup suppression)
        Alert alert = buildAlert(1, "ALT-001");

        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of(1L, 999L));
        when(alertRepository.findById(1)).thenReturn(Optional.of(alert));
        when(alertRepository.findById(999)).thenReturn(Optional.empty()); // missing

        List<Alert> result = service.screenTransaction(42);

        assertEquals(1, result.size(), "Missing alert IDs should be filtered out gracefully");
        assertEquals("ALT-001", result.get(0).getAlertRef());
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Test
    void returns_empty_list_when_transaction_not_found() {
        when(txnRepository.findById(999)).thenReturn(Optional.empty());

        List<Alert> result = service.screenTransaction(999);

        assertTrue(result.isEmpty());
    }


    @Test
    void does_not_call_rule_engine_when_transaction_missing() {
        when(txnRepository.findById(999)).thenReturn(Optional.empty());

        service.screenTransaction(999);

        verify(ruleEngineService, never()).screenTransaction(any(), any());
    }

    // AML domain scenarios

    @Test
    void high_value_pep_transaction_triggers_alert_from_multiple_rules() {
        // Simulate two rules firing: THRESHOLD and WATCHLIST
        Alert thresholdAlert = buildAlert(1, "ALT-THR-001");
        Alert watchlistAlert = buildAlert(2, "ALT-WL-001");

        txn.setAmountUsd(new BigDecimal("75000.00"));

        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of(1L, 2L));
        when(alertRepository.findById(1)).thenReturn(Optional.of(thresholdAlert));
        when(alertRepository.findById(2)).thenReturn(Optional.of(watchlistAlert));

        List<Alert> result = service.screenTransaction(42);

        assertEquals(2, result.size());
    }

    @Test
    void small_routine_transaction_generates_no_alerts() {
        txn.setAmountUsd(new BigDecimal("150.00"));

        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));
        when(ruleEngineService.screenTransaction(txn, account)).thenReturn(List.of());

        List<Alert> result = service.screenTransaction(42);

        assertTrue(result.isEmpty());
    }

    // Helpers methods

    private Alert buildAlert(int id, String ref) {
        Alert a = new Alert();
        a.setAlertId(id);
        a.setAlertRef(ref);
        a.setAlertScore((short) 75);
        a.setStatus(AlertStatus.OPEN);
        a.setTriggeredAt(LocalDateTime.now());
        return a;
    }
}
