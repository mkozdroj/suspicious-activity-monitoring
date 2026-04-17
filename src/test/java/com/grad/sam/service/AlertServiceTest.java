package com.grad.sam.service;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.exception.DataNotFoundException;
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
}
