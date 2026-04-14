package com.grad.sam.rules;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class VelocityRuleTest {

    private VelocityRule rule;
    private AlertRule alertRule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new VelocityRule();

        alertRule = new AlertRule();
        alertRule.setRuleId(2);
        alertRule.setRuleCode("VEL-001");
        alertRule.setRuleName("High Velocity");
        alertRule.setRuleCategory(RuleCategory.VELOCITY);
        alertRule.setSeverity(AlertSeverity.MEDIUM);
        alertRule.setThresholdCount(10);   // more than 10 txns in window → flag
        alertRule.setLookbackDays(7);
        alertRule.setIsActive(true);

        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-0002");
    }

    // happy path
    @Test
    void fires_when_total_count_exceeds_threshold() {
        // 10 recent + 1 current = 11 > 10
        List<Txn> recent = buildTxns(10);
        RuleContext ctx = buildContext(recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Expected alert: 11 txns > threshold of 10");
    }

    @Test
    void does_not_fire_when_total_equals_threshold() {
        // 9 recent + 1 current = 10, NOT > 10
        List<Txn> recent = buildTxns(9);
        RuleContext ctx = buildContext(recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should NOT alert when total == threshold (must be strictly greater)");
    }

    @Test
    void does_not_fire_when_total_below_threshold() {
        List<Txn> recent = buildTxns(5);
        RuleContext ctx = buildContext(recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should NOT alert when count < threshold");
    }

    @Test
    void fires_with_no_recent_txns_if_threshold_is_zero() {
        alertRule.setThresholdCount(0); // any transaction exceeds threshold of 0
        RuleContext ctx = buildContext(Collections.emptyList());

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "1 txn > threshold of 0 should fire");
    }

    // edge cases
    @Test
    void returns_empty_when_threshold_count_is_null() {
        alertRule.setThresholdCount(null);
        RuleContext ctx = buildContext(buildTxns(99));

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should return empty when rule has no count threshold configured");
    }

    @Test
    void reason_contains_account_number() {
        RuleContext ctx = buildContext(buildTxns(10));

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("ACC-0002"));
    }

    @Test
    void reason_contains_lookback_days() {
        RuleContext ctx = buildContext(buildTxns(10));

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("7"), "Reason should mention the lookback window");
    }

    @Test
    void reason_contains_total_count() {
        // 10 recent + 1 = 11
        RuleContext ctx = buildContext(buildTxns(10));

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("11"), "Reason should state total transaction count");
    }

    @Test
    void supported_category_is_velocity() {
        assertEquals("VELOCITY", rule.getSupportedCategory());
    }

    // helpers methods
    private List<Txn> buildTxns(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    Txn txn = new Txn();
                    txn.setTxnId(i + 100);
                    txn.setAmountUsd(new BigDecimal("500.00"));
                    return txn;
                })
                .toList();
    }

    private RuleContext buildContext(List<Txn> recentTxns) {
        Txn current = new Txn();
        current.setTxnId(1);
        current.setTxnRef("TXN-VEL-001");
        current.setAmountUsd(new BigDecimal("500.00"));

        return RuleContext.builder()
                .txn(current)
                .account(account)
                .recentTxns(recentTxns)
                .build();
    }
}
