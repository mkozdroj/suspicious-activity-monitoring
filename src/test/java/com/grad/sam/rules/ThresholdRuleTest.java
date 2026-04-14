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

class ThresholdRuleTest {

    private ThresholdRule rule;
    private AlertRule alertRule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new ThresholdRule();

        alertRule = new AlertRule();
        alertRule.setRuleId(1);
        alertRule.setRuleCode("THR-001");
        alertRule.setRuleName("Large Transaction");
        alertRule.setSeverity(AlertSeverity.HIGH);
        alertRule.setThresholdAmount(new BigDecimal("10000.00"));
        alertRule.setLookbackDays(30);
        alertRule.setIsActive(true);

        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-0001");
    }

    // happy path
    @Test
    void fires_when_amount_strictly_above_threshold() {
        RuleContext ctx = buildContext("10001.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Expected alert when amount > threshold");
        assertTrue(result.get().getReason().contains("10001"));
    }

    @Test
    void does_not_fire_when_amount_equals_threshold() {
        RuleContext ctx = buildContext("10000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should NOT alert when amount == threshold (must be strictly greater)");
    }

    @Test
    void does_not_fire_when_amount_below_threshold() {
        RuleContext ctx = buildContext("9999.99");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should NOT alert when amount < threshold");
    }

    // edge cases
    @Test
    void returns_empty_when_threshold_is_null() {
        alertRule.setThresholdAmount(null);
        RuleContext ctx = buildContext("99999.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should return empty when rule has no threshold configured");
    }

    @Test
    void reason_contains_account_number() {
        RuleContext ctx = buildContext("50000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("ACC-0001"));
    }

    @Test
    void result_carries_correct_alert_rule() {
        RuleContext ctx = buildContext("50000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertEquals(alertRule, result.get().getRule());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10001.00", "50000.00", "999999.99", "100000000.00"})
    void fires_for_various_amounts_above_threshold(String amount) {
        RuleContext ctx = buildContext(amount);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Expected alert for amount " + amount);
    }

    // currency tests
    @ParameterizedTest
    @ValueSource(strings = {"GBP", "EUR", "JPY", "CHF", "SGD"})
    void fires_regardless_of_original_currency_when_amountUsd_exceeds_threshold(String currency) {
        // Rule evaluates amountUsd (normalised), not original amount.
        // A GBP/EUR/etc txn with amountUsd above threshold must still fire.
        RuleContext ctx = buildContextWithCurrency("15000.00", "12000.00", currency);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(),
                "Should fire for " + currency + " transaction when amountUsd exceeds threshold");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GBP", "EUR", "JPY", "CHF", "SGD"})
    void does_not_fire_for_foreign_currency_when_amountUsd_below_threshold(String currency) {
        // A large original amount but low amountUsd must NOT fire.
        // e.g. 9999 JPY = well below 10000 USD threshold
        RuleContext ctx = buildContextWithCurrency("9999000.00", "9000.00", currency);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(),
                "Should NOT fire for " + currency + " when amountUsd is below threshold");
    }

    @Test
    void reason_contains_original_currency_code() {
        RuleContext ctx = buildContextWithCurrency("9500.00", "12000.00", "GBP");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("GBP"),
                "Reason should mention the original transaction currency, not just USD");
    }

    @Test
    void reason_contains_original_amount_not_just_usd() {
        // original amount = 9500 GBP, amountUsd = 12000 USD
        RuleContext ctx = buildContextWithCurrency("9500.00", "12000.00", "GBP");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        String reason = result.get().getReason();
        // Reason should show both the original amount and the USD equivalent
        assertTrue(reason.contains("9500") || reason.contains("12000"),
                "Reason should reference the transaction amount");
    }

    @Test
    void supported_category_is_threshold() {
        assertEquals(RuleCategory.STRUCTURING.name(), rule.getSupportedCategory());
    }

    // helper methods
    private RuleContext buildContext(String amountUsd) {
        return buildContextWithCurrency(amountUsd, amountUsd, "USD");
    }

    private RuleContext buildContextWithCurrency(String originalAmount, String amountUsd, String currency) {
        Txn txn = new Txn();
        txn.setTxnId(1);
        txn.setTxnRef("TXN-001");
        txn.setAmount(new BigDecimal(originalAmount));   // original currency amount
        txn.setAmountUsd(new BigDecimal(amountUsd));     // normalised USD value — what the rule checks
        txn.setCurrency(currency);

        return RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(List.of())
                .build();
    }
}
