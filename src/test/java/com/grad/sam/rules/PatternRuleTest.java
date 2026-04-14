package com.grad.sam.rules;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


// PatternRule (Smurfing detection) fires when the same amount appears
// >= thresholdCount times within the lookback window (including the current txn).
@ActiveProfiles("test")
class PatternRuleTest {

    private PatternRule rule;
    private AlertRule alertRule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new PatternRule();

        alertRule = new AlertRule();
        alertRule.setRuleId(6);
        alertRule.setRuleCode("PAT-001");
        alertRule.setRuleName("Smurfing / Pattern Detection");
        alertRule.setRuleCategory(RuleCategory.PATTERN);
        alertRule.setSeverity(AlertSeverity.HIGH);
        alertRule.setThresholdCount(3);   // same amount repeated >= 3 times → flag
        alertRule.setLookbackDays(14);
        alertRule.setIsActive(true);

        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-0006");
    }

    @Test
    void fires_when_same_amount_repeated_at_threshold_count() {
        // 2 recent + 1 current = 3 occurrences of 4999.00 → fires (>= 3)
        List<Txn> recent = buildTxnsWithAmount(2, "4999.00");
        RuleContext ctx = buildContext("4999.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Same amount 3 times should fire");
    }

    @Test
    void fires_when_same_amount_repeated_above_threshold_count() {
        // 4 occurrences > threshold of 3
        List<Txn> recent = buildTxnsWithAmount(4, "4999.00");
        RuleContext ctx = buildContext("4999.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "4 occurrences of same amount should fire");
    }

    @Test
    void reason_mentions_repeated_amount() {
        List<Txn> recent = buildTxnsWithAmount(2, "4999.00");
        RuleContext ctx = buildContext("4999.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("4999"), "Reason should mention the repeated amount");
    }

    @Test
    void reason_mentions_occurrence_count() {
        List<Txn> recent = buildTxnsWithAmount(2, "4999.00");
        RuleContext ctx = buildContext("4999.00", recent);  // 3 total

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("3"), "Reason should mention how many times the amount appeared");
    }

    // ── no smurfing ──────────────────────────────────────────────────────────

    @Test
    void does_not_fire_when_count_is_below_threshold() {
        // 1 recent + 1 current = 2 < 3 threshold
        List<Txn> recent = buildTxnsWithAmount(1, "4999.00");
        RuleContext ctx = buildContext("4999.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "2 occurrences < threshold 3 — should not fire");
    }

    @Test
    void does_not_fire_when_all_amounts_are_different() {
        List<Txn> recent = List.of(
                buildTxn(201, "1000.00"),
                buildTxn(202, "2000.00"),
                buildTxn(203, "3000.00")
        );
        RuleContext ctx = buildContext("4000.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "All different amounts — no pattern, should not fire");
    }

    @Test
    void does_not_fire_with_no_recent_transactions() {
        // Only 1 occurrence (the current txn itself) — below threshold of 3
        RuleContext ctx = buildContext("9999.00", List.of());

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Single txn, no history — should not fire");
    }

    // ── default threshold ─────────────────────────────────────────────────────

    @Test
    void uses_default_threshold_of_2_when_rule_has_null_count() {
        alertRule.setThresholdCount(null);  // falls back to DEFAULT_REPEAT_THRESHOLD = 2
        // 1 recent + 1 current = 2 occurrences → fires at default threshold
        List<Txn> recent = buildTxnsWithAmount(1, "5000.00");
        RuleContext ctx = buildContext("5000.00", recent);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Default threshold is 2 — 2 same amounts should fire");
    }

    @Test
    void does_not_fire_with_default_threshold_when_only_one_occurrence() {
        alertRule.setThresholdCount(null);
        RuleContext ctx = buildContext("5000.00", List.of());

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "1 occurrence < default threshold 2 — should not fire");
    }

    // ── mixed window — only repeated amount flags ─────────────────────────────

    @Test
    void fires_only_for_the_repeated_amount_ignoring_others() {
        // Mix: two unique amounts + the repeat
        List<Txn> recent = List.of(
                buildTxn(201, "100.00"),
                buildTxn(202, "200.00"),
                buildTxn(203, "4999.00"),
                buildTxn(204, "4999.00")
        );
        RuleContext ctx = buildContext("4999.00", recent);  // 3 occurrences of 4999

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "4999 repeated 3 times should fire even with other amounts present");
        assertTrue(result.get().getReason().contains("4999"));
    }

    @Test
    void supported_category_is_pattern() {
        assertEquals("PATTERN", rule.getSupportedCategory());
    }


    // helper methods
    private List<Txn> buildTxnsWithAmount(int count, String amountUsd) {
        return IntStream.range(0, count)
                .mapToObj(i -> buildTxn(300 + i, amountUsd))
                .toList();
    }

    private Txn buildTxn(int id, String amountUsd) {
        Txn txn = new Txn();
        txn.setTxnId(id);
        txn.setAmountUsd(new BigDecimal(amountUsd));
        return txn;
    }

    private RuleContext buildContext(String currentAmountUsd, List<Txn> recentTxns) {
        Txn current = new Txn();
        current.setTxnId(1);
        current.setTxnRef("TXN-PAT-001");
        current.setAmountUsd(new BigDecimal(currentAmountUsd));

        return RuleContext.builder()
                .txn(current)
                .account(account)
                .recentTxns(recentTxns)
                .build();
    }
}
