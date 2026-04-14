package com.grad.sam.rules;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Structuring rule fires when the cumulative USD total in the lookback window
// falls in the band [75% of threshold, 100% of threshold).
// i.e. the customer is breaking up payments to stay just under the reporting limit.

class StructuringRuleTest {

    private StructuringRule rule;
    private AlertRule alertRule;
    private Account account;

    // Threshold = 10 000 USD; band = [7 500, 10 000)
    private static final BigDecimal THRESHOLD = new BigDecimal("10000.00");

    @BeforeEach
    void setUp() {
        rule = new StructuringRule();

        alertRule = new AlertRule();
        alertRule.setRuleId(3);
        alertRule.setRuleCode("STR-001");
        alertRule.setRuleName("Structuring Detection");
        alertRule.setRuleCategory(RuleCategory.STRUCTURING);
        alertRule.setSeverity(AlertSeverity.HIGH);
        alertRule.setThresholdAmount(THRESHOLD);
        alertRule.setLookbackDays(30);
        alertRule.setIsActive(true);

        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-0003");
    }

    // core band logic
    @Test
    void fires_when_cumulative_total_is_at_lower_band_exactly() {
        // 4 x 1875 = 7500 (exactly 75% of 10 000) → fires
        List<Txn> recent = buildTxns(3, "1875.00");          // 3 recent
        RuleContext ctx = buildContext("1875.00", recent);   // + 1 current = 7500

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "7500 == 75% threshold — should fire");
    }

    @Test
    void fires_when_cumulative_total_is_inside_band() {
        // 3 x 2500 + 1000 = 8500 → in [7500, 10000)
        List<Txn> recent = buildTxns(3, "2500.00");
        RuleContext ctx = buildContext("1000.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "8500 is within structuring band — should fire");
    }

    @Test
    void does_not_fire_when_cumulative_total_reaches_threshold() {
        // 4 x 2500 = 10000 → exactly at threshold, NOT structuring (that's a large txn)
        List<Txn> recent = buildTxns(3, "2500.00");
        RuleContext ctx = buildContext("2500.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "10000 == threshold — not structuring (ThresholdRule handles that)");
    }

    @Test
    void does_not_fire_when_cumulative_total_exceeds_threshold() {
        // total > threshold → ThresholdRule domain, not structuring
        List<Txn> recent = buildTxns(3, "4000.00");
        RuleContext ctx = buildContext("4000.00", recent);  // 16000 > 10000

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Total above threshold is not structuring");
    }

    @Test
    void does_not_fire_when_cumulative_total_is_below_lower_band() {
        // total = 5000 < 7500
        List<Txn> recent = buildTxns(2, "1500.00");
        RuleContext ctx = buildContext("2000.00", recent);  // 3000 + 2000 = 5000

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "5000 < lower band 7500 — should not fire");
    }

    // edge cases
    @Test
    void returns_empty_when_threshold_is_null() {
        alertRule.setThresholdAmount(null);
        RuleContext ctx = buildContext("9000.00", List.of());

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent());
    }

    @Test
    void fires_with_only_current_transaction_in_band() {
        // No recent txns, current txn alone = 8000 → in [7500, 10000)
        RuleContext ctx = buildContext("8000.00", List.of());

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Single txn of 8000 within band — should fire");
    }

    @Test
    void reason_mentions_lookback_days() {
        List<Txn> recent = buildTxns(3, "2000.00");
        RuleContext ctx = buildContext("2000.00", recent);  // total 8000

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("30"), "Reason should mention lookback window");
    }

    @Test
    void supported_category_is_structuring() {
        assertEquals("STRUCTURING", rule.getSupportedCategory());
    }

    // helper methods
    private List<Txn> buildTxns(int count, String amountUsd) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    Txn txn = new Txn();
                    txn.setTxnId(i + 200);
                    txn.setAmountUsd(new BigDecimal(amountUsd));
                    return txn;
                })
                .toList();
    }

    private RuleContext buildContext(String currentAmountUsd, List<Txn> recentTxns) {
        Txn current = new Txn();
        current.setTxnId(1);
        current.setTxnRef("TXN-STR-001");
        current.setAmountUsd(new BigDecimal(currentAmountUsd));
        current.setAmount(new BigDecimal(currentAmountUsd));
        current.setCurrency("USD");

        return RuleContext.builder()
                .txn(current)
                .account(account)
                .recentTxns(recentTxns)
                .build();
    }
}
