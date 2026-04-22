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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
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

    @Test
    void fires_when_amount_above_threshold() {
        RuleContext ctx = buildContext("12000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Expected alert when amountUsd > threshold");
        assertEquals(alertRule, result.get().getRule());
    }

    @Test
    void does_not_fire_when_amount_equals_threshold() {
        RuleContext ctx = buildContext("10000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should not alert when amountUsd == threshold");
    }

    @Test
    void does_not_fire_when_amount_below_threshold() {
        RuleContext ctx = buildContext("9999.99");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should not alert when amountUsd < threshold");
    }

    @Test
    void returns_empty_when_threshold_is_null() {
        alertRule.setThresholdAmount(null);
        RuleContext ctx = buildContext("50000.00");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should return empty when thresholdAmount is null");
    }

    @Test
    void uses_amount_usd_for_foreign_currency_transaction() {
        RuleContext ctx = buildContextWithCurrency("9500.00", "12000.00", "GBP");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(),
                "Should fire because amountUsd exceeds threshold even for non-USD transaction");
    }

    @Test
    void does_not_fire_for_foreign_currency_transaction_when_amount_usd_below_threshold() {
        RuleContext ctx = buildContextWithCurrency("15000.00", "9000.00", "EUR");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(),
                "Should not fire because amountUsd is below threshold");
    }

    @Test
    void reason_contains_original_currency_original_amount_usd_amount_and_account_number() {
        RuleContext ctx = buildContextWithCurrency("9500.00", "12000.00", "GBP");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());

        String reason = result.get().getReason();
        assertTrue(reason.contains("GBP"));
        assertTrue(reason.contains("9500.00") || reason.contains("9500"));
        assertTrue(reason.contains("12000.00") || reason.contains("12000"));
        assertTrue(reason.contains("10000.00") || reason.contains("10000"));
        assertTrue(reason.contains("ACC-0001"));
    }

    @Test
    void supported_category_is_structuring() {
        assertEquals(RuleCategory.STRUCTURING.name(), rule.getSupportedCategory());
    }

    @Test
    void supports_returns_true_for_threshold_rule_code() {
        assertTrue(rule.supports(alertRule));
    }

    @Test
    void supports_returns_false_for_null_rule() {
        assertFalse(rule.supports(null));
    }

    @Test
    void supports_returns_false_when_rule_code_is_null() {
        alertRule.setRuleCode(null);
        assertFalse(rule.supports(alertRule));
    }

    @Test
    void supports_returns_false_for_non_threshold_rule_code() {
        alertRule.setRuleCode("STR-001");
        assertFalse(rule.supports(alertRule));
    }

    @Test
    void throws_when_context_is_null() {
        assertThrows(IllegalArgumentException.class, () -> rule.evaluate(null, alertRule));
    }

    @Test
    void throws_when_rule_is_null() {
        assertThrows(IllegalArgumentException.class, () -> rule.evaluate(buildContext("1000.00"), null));
    }

    @Test
    void throws_when_transaction_is_null() {
        RuleContext ctx = RuleContext.builder()
                .account(account)
                .recentTxns(List.of())
                .build();

        assertThrows(IllegalArgumentException.class, () -> rule.evaluate(ctx, alertRule));
    }

    @Test
    void throws_when_account_is_null() {
        Txn txn = new Txn();
        txn.setTxnId(1);
        txn.setTxnRef("TXN-001");
        txn.setAmount(new BigDecimal("1000.00"));
        txn.setAmountUsd(new BigDecimal("1000.00"));
        txn.setCurrency("USD");

        RuleContext ctx = RuleContext.builder()
                .txn(txn)
                .account(null)
                .recentTxns(List.of())
                .build();

        assertThrows(IllegalArgumentException.class, () -> rule.evaluate(ctx, alertRule));
    }

    @Test
    void throws_when_amount_usd_is_null() {
        Txn txn = new Txn();
        txn.setTxnId(1);
        txn.setTxnRef("TXN-001");
        txn.setAmount(new BigDecimal("1000.00"));
        txn.setCurrency("USD");

        RuleContext ctx = RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(List.of())
                .build();

        assertThrows(IllegalStateException.class, () -> rule.evaluate(ctx, alertRule));
    }

    private RuleContext buildContext(String amountUsd) {
        return buildContextWithCurrency(amountUsd, amountUsd, "USD");
    }

    private RuleContext buildContextWithCurrency(String originalAmount, String amountUsd, String currency) {
        Txn txn = new Txn();
        txn.setTxnId(1);
        txn.setTxnRef("TXN-001");
        txn.setAmount(new BigDecimal(originalAmount));
        txn.setAmountUsd(new BigDecimal(amountUsd));
        txn.setCurrency(currency);

        return RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(List.of())
                .build();
    }
}