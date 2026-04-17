package com.grad.sam.service;

import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.*;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AlertRaisingServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private TxnRepository txnRepository;
    @Mock private DataSource dataSource;
    @Mock private WatchlistScreeningService watchlistScreeningService;


    @Mock private Connection raiseConn;
    @Mock private Connection screenConn;

    @Mock private CallableStatement raiseStmt;
    @Mock private CallableStatement screenStmt;

    private RuleEngineService service;
    private Txn txn;
    private Account account;

    @BeforeEach
    void setup() {
        account = new Account();
        account.setAccountId(1);

        txn = new Txn();
        txn.setTxnId(10);
        txn.setTxnRef("TXN-1");
        txn.setAmountUsd(new BigDecimal("15000"));
    }

    // 1. Alert is raiseed
    @Test
    void should_raise_alert_when_rule_matches() throws Exception {

        stubOneRaise();

        AlertRule rule = buildRule();
        AmlRule impl = mockRuleFires(rule, "Large txn");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(raiseStmt.getLong(4)).thenReturn(100L);

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(txn, account);

        assertEquals(1, result.size());
        assertEquals(100L, result.getFirst());

        verify(raiseStmt).execute();
    }

    // 2. Duplicate supression
    @Test
    void should_not_return_alert_when_duplicate_detected() throws Exception {

        stubOneRaise();

        AlertRule rule = buildRule();
        AmlRule impl = mockRuleFires(rule, "Duplicate txn");

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule));
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        when(raiseStmt.getLong(4)).thenReturn(-1L); // DUPLICATE

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(impl), watchlistScreeningService);

        List<Long> result = service.screenTransaction(txn, account);

        assertTrue(result.isEmpty());
    }

    // 3. Screened flag
    @Test
    void should_always_call_screen_transaction() throws Exception {

        when(dataSource.getConnection()).thenReturn(screenConn);
        when(screenConn.prepareCall("{CALL screen_transaction(?)}")).thenReturn(screenStmt);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        service = new RuleEngineService(alertRuleRepository, txnRepository, dataSource, List.of(), watchlistScreeningService);

        service.screenTransaction(txn, account);

        verify(screenStmt).execute();
    }

    // Helper methods
    private void stubOneRaise() throws Exception {
        when(dataSource.getConnection()).thenReturn(raiseConn, screenConn);

        when(raiseConn.prepareCall("{CALL raise_alert(?, ?, ?, ?)}"))
                .thenReturn(raiseStmt);

        when(screenConn.prepareCall("{CALL screen_transaction(?)}"))
                .thenReturn(screenStmt);
    }

    private AlertRule buildRule() {
        AlertRule r = new AlertRule();
        r.setRuleId(1);
        r.setRuleCode("VEL-1");
        r.setRuleCategory(RuleCategory.VELOCITY);
        r.setThresholdAmount(new BigDecimal("10000"));
        r.setLookbackDays(30);
        return r;
    }

    private AmlRule mockRuleFires(AlertRule rule, String reason) {
        AmlRule impl = mock(AmlRule.class);
        when(impl.getSupportedCategory()).thenReturn(rule.getRuleCategory().name());
        when(impl.evaluate(any(RuleContext.class), eq(rule)))
                .thenReturn(Optional.of(new RuleMatch(rule, reason)));
        return impl;
    }
}