package com.grad.sam.rules;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// RoundNumberRule fires when amountUsd is exactly divisible by the divisor (default 1000)
// AND amountUsd >= divisor.
// Rationale: criminals often use round numbers to avoid attention.

class RoundNumberRuleTest {

    private RoundNumberRule rule;
    private AlertRule alertRule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new RoundNumberRule();

        alertRule = new AlertRule();
        alertRule.setRuleId(5);
        alertRule.setRuleCode("RND-001");
        alertRule.setRuleName("Round Number Detection");
        alertRule.setSeverity(AlertSeverity.LOW);
        alertRule.setThresholdAmount(null);  // uses default divisor 1000
        alertRule.setLookbackDays(30);
        alertRule.setIsActive(true);

        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-0005");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000.00", "2000.00", "5000.00", "10000.00", "50000.00", "100000.00"})
    void fires_for_round_multiples_of_1000(String amount) {
        RuleContext ctx = buildContext(amount);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Expected alert for round amount: " + amount);
    }

    @Test
    void fires_exactly_at_minimum_divisor() {
        // 1000 is the minimum — exactly divisible and >= 1000
        RuleContext ctx = buildContext("1000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "1000 should fire — exactly at minimum threshold");
    }

    // ── non-round amounts that should not fire ───────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"1500.00", "9999.99", "1001.00", "3456.78", "750.00"})
    void does_not_fire_for_non_round_amounts(String amount) {
        RuleContext ctx = buildContext(amount);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should NOT alert for non-round amount: " + amount);
    }

    @Test
    void does_not_fire_when_amount_is_below_minimum_divisor() {
        // 500 is divisible by 500 (not 1000) — and below 1000
        RuleContext ctx = buildContext("500.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "500 < 1000 minimum — should not fire");
    }

    @Test
    void does_not_fire_for_zero() {
        RuleContext ctx = buildContext("0.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Zero amount — should not fire");
    }

    // custom divisor from rule threshold
    void fires_for_custom_divisor_set_in_rule() {
        // Rule configured with threshold 500 instead of default 1000
        alertRule.setThresholdAmount(new BigDecimal("500.00"));
        RuleContext ctx = buildContext("1500.00");  // 1500 divisible by 500

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "1500 divisible by custom divisor 500 — should fire");
    }

    @Test
    void does_not_fire_for_custom_divisor_when_not_divisible() {
        alertRule.setThresholdAmount(new BigDecimal("500.00"));
        RuleContext ctx = buildContext("1300.00");  // 1300 not divisible by 500

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "1300 not divisible by 500 — should not fire");
    }

    @Test
    void falls_back_to_default_divisor_when_threshold_is_zero() {
        alertRule.setThresholdAmount(BigDecimal.ZERO);
        RuleContext ctx = buildContext("3000.00");  // 3000 divisible by default 1000

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Zero threshold should fall back to default 1000 divisor");
    }


    // reason quality
    @Test
    void reason_mentions_round_amount() {
        RuleContext ctx = buildContext("5000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("5000"), "Reason should mention the amount");
    }

    @Test
    void supported_category_is_round_number() {
        assertEquals(RuleCategory.PATTERN.name(), rule.getSupportedCategory());
    }

    // helper methods
    private RuleContext buildContext(String amountUsd) {
        Txn txn = new Txn();
        txn.setTxnId(1);
        txn.setTxnRef("TXN-RND-001");
        txn.setAmountUsd(new BigDecimal(amountUsd));
        txn.setAmount(new BigDecimal(amountUsd));
        txn.setCurrency("USD");

        return RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(List.of())
                .build();
    }
}
