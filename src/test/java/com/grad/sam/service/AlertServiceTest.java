package com.grad.sam.service;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.model.*;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TxnRepository txnRepository;

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @InjectMocks
    private AlertService service;

    private Alert existingAlert;

    @BeforeEach
    void setUp() {
        existingAlert = new Alert();
        existingAlert.setAlertId(10);
        existingAlert.setAlertRef("ALT-010");
        existingAlert.setStatus(AlertStatus.OPEN);
    }

    @Test
    void raiseAlert_delegates_to_repository() {
        Integer txnId = 42;

        Account account = new Account();
        Txn txn = new Txn();
        txn.setTxnId(txnId);
        txn.setAccount(account);

        AlertRule rule = new AlertRule();
        rule.setRuleCategory(RuleCategory.WATCHLIST);
        rule.setSeverity(AlertSeverity.HIGH);

        when(txnRepository.findById(txnId)).thenReturn(Optional.of(txn));
        when(alertRuleRepository.findByRuleCategoryAndIsActiveTrue(RuleCategory.WATCHLIST))
                .thenReturn(List.of(rule));

        Alert savedAlert = new Alert();
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        Alert result = service.raiseAlert(txnId, "WATCHLIST", "Exact watchlist match");

        assertSame(savedAlert, result);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        Alert alertToSave = captor.getValue();
        assertEquals(txn, alertToSave.getTxn());
        assertEquals(account, alertToSave.getAccount());
        assertEquals(rule, alertToSave.getAlertRule());
        assertEquals("Exact watchlist match", alertToSave.getNotes());
        assertEquals((short) 75, alertToSave.getAlertScore());
        assertNotNull(alertToSave.getTriggeredAt());
        assertNotNull(alertToSave.getAlertRef());

        verify(txnRepository).findById(txnId);
        verify(alertRuleRepository).findByRuleCategoryAndIsActiveTrue(RuleCategory.WATCHLIST);
        verify(alertRepository).save(any(Alert.class));
        verifyNoMoreInteractions(alertRepository, txnRepository, alertRuleRepository);
    }

    @Test
    void raiseAlert_throws_when_transaction_missing() {
        when(txnRepository.findById(42)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class,
                () -> service.raiseAlert(42, "WATCHLIST", "Exact watchlist match"));

        verify(txnRepository).findById(42);
        verifyNoInteractions(alertRepository, alertRuleRepository);
    }

    @Test
    void raiseAlert_throws_when_transaction_has_no_account() {
        Txn txn = new Txn();
        txn.setTxnId(42);
        txn.setAccount(null);
        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));

        assertThrows(IllegalStateException.class,
                () -> service.raiseAlert(42, "WATCHLIST", "Exact watchlist match"));

        verify(txnRepository).findById(42);
        verifyNoInteractions(alertRepository, alertRuleRepository);
    }

    @Test
    void raiseAlert_throws_when_alert_type_is_unsupported() {
        Account account = new Account();
        Txn txn = new Txn();
        txn.setTxnId(42);
        txn.setAccount(account);
        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));

        assertThrows(InvalidInputException.class,
                () -> service.raiseAlert(42, "NOT_A_REAL_TYPE", "Exact watchlist match"));

        verify(txnRepository).findById(42);
        verifyNoInteractions(alertRepository, alertRuleRepository);
    }

    @Test
    void raiseAlert_accepts_lowercase_alert_type() {
        Integer txnId = 42;
        Account account = new Account();
        Txn txn = new Txn();
        txn.setTxnId(txnId);
        txn.setAccount(account);

        AlertRule rule = new AlertRule();
        rule.setRuleCategory(RuleCategory.WATCHLIST);
        rule.setSeverity(AlertSeverity.HIGH);

        when(txnRepository.findById(txnId)).thenReturn(Optional.of(txn));
        when(alertRuleRepository.findByRuleCategoryAndIsActiveTrue(RuleCategory.WATCHLIST))
                .thenReturn(List.of(rule));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = service.raiseAlert(txnId, "watchlist", "Exact watchlist match");

        assertNotNull(result);
        assertEquals(rule, result.getAlertRule());
        verify(txnRepository).findById(txnId);
        verify(alertRuleRepository).findByRuleCategoryAndIsActiveTrue(RuleCategory.WATCHLIST);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void raiseAlert_throws_when_no_active_rule_exists_for_category() {
        Integer txnId = 42;
        Account account = new Account();
        Txn txn = new Txn();
        txn.setTxnId(txnId);
        txn.setAccount(account);

        when(txnRepository.findById(txnId)).thenReturn(Optional.of(txn));
        when(alertRuleRepository.findByRuleCategoryAndIsActiveTrue(RuleCategory.WATCHLIST))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> service.raiseAlert(txnId, "WATCHLIST", "Exact watchlist match"));

        verify(txnRepository).findById(txnId);
        verify(alertRuleRepository).findByRuleCategoryAndIsActiveTrue(RuleCategory.WATCHLIST);
        verifyNoInteractions(alertRepository);
    }

    @Test
    void createAlert_truncates_notes_longer_than_500_characters() {
        Txn txn = new Txn();
        txn.setTxnId(1);
        Account account = new Account();
        AlertRule rule = buildRule(AlertSeverity.HIGH);
        String longDescription = "x".repeat(501);

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = service.createAlert(txn, account, rule, longDescription);

        assertNotNull(result.getNotes());
        assertEquals(500, result.getNotes().length());
        assertTrue(result.getNotes().endsWith("..."));
    }

    @Test
    void createAlert_keeps_notes_unchanged_when_length_is_500() {
        Txn txn = new Txn();
        txn.setTxnId(1);
        Account account = new Account();
        AlertRule rule = buildRule(AlertSeverity.HIGH);
        String description = "x".repeat(500);

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = service.createAlert(txn, account, rule, description);

        assertEquals(description, result.getNotes());
    }

    @Test
    void createAlert_keeps_null_notes_as_null() {
        Txn txn = new Txn();
        txn.setTxnId(1);
        Account account = new Account();
        AlertRule rule = buildRule(AlertSeverity.HIGH);

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = service.createAlert(txn, account, rule, null);

        assertNull(result.getNotes());
    }

    @Test
    void createAlert_resolves_low_severity_to_25() {
        Alert result = createAlertWithSeverity(AlertSeverity.LOW);
        assertEquals((short) 25, result.getAlertScore());
    }

    @Test
    void createAlert_resolves_medium_severity_to_50() {
        Alert result = createAlertWithSeverity(AlertSeverity.MEDIUM);
        assertEquals((short) 50, result.getAlertScore());
    }

    @Test
    void createAlert_resolves_critical_severity_to_100() {
        Alert result = createAlertWithSeverity(AlertSeverity.CRITICAL);
        assertEquals((short) 100, result.getAlertScore());
    }

    @Test
    void createAlert_defaults_null_severity_to_50() {
        Alert result = createAlertWithSeverity(null);
        assertEquals((short) 50, result.getAlertScore());
    }

    @Test
    void updateStatus_updates_alert_and_saves() {
        when(alertRepository.findById(10)).thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(existingAlert)).thenReturn(existingAlert);

        Alert result = service.updateStatus(10, AlertStatus.CLOSED);

        assertSame(existingAlert, result);
        assertEquals(AlertStatus.CLOSED, existingAlert.getStatus());
        verify(alertRepository).findById(10);
        verify(alertRepository).save(existingAlert);
    }

    @Test
    void updateStatus_throws_when_alert_missing() {
        when(alertRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class, () -> service.updateStatus(999, AlertStatus.CLOSED));
        verify(alertRepository).findById(999);
        verify(alertRepository, never()).save(any());
    }

    @Test
    void assignTo_moves_open_alert_to_under_review() {
        when(alertRepository.findById(10)).thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(existingAlert)).thenReturn(existingAlert);

        Alert result = service.assignTo(10, "analyst@bank.com");

        assertSame(existingAlert, result);
        assertEquals("analyst@bank.com", existingAlert.getAssignedTo());
        assertEquals(AlertStatus.UNDER_REVIEW, existingAlert.getStatus());
        verify(alertRepository).findById(10);
        verify(alertRepository).save(existingAlert);
    }

    @Test
    void assignTo_keeps_existing_non_open_status() {
        existingAlert.setStatus(AlertStatus.SAR_FILED);
        when(alertRepository.findById(10)).thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(existingAlert)).thenReturn(existingAlert);

        Alert result = service.assignTo(10, "analyst@bank.com");

        assertSame(existingAlert, result);
        assertEquals("analyst@bank.com", existingAlert.getAssignedTo());
        assertEquals(AlertStatus.SAR_FILED, existingAlert.getStatus());
        verify(alertRepository).findById(10);
        verify(alertRepository).save(existingAlert);
    }

    @Test
    void deleteAlert_deletes_by_id() {
        service.deleteAlert(10);

        verify(alertRepository).deleteById(10);
    }

    private Alert createAlertWithSeverity(AlertSeverity severity) {
        Txn txn = new Txn();
        txn.setTxnId(1);
        Account account = new Account();
        AlertRule rule = buildRule(severity);

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        return service.createAlert(txn, account, rule, "desc");
    }

    private AlertRule buildRule(AlertSeverity severity) {
        AlertRule rule = new AlertRule();
        rule.setRuleCategory(RuleCategory.WATCHLIST);
        rule.setSeverity(severity);
        return rule;
    }
}
