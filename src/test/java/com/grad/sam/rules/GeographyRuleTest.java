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
class GeographyRuleTest {

    private GeographyRule rule;
    private AlertRule alertRule;
    private Account account;

    // Default high-risk countries from GeographyRule
    // IR, KP, SY, CU, MM, BY, RU, VE
    @BeforeEach
    void setUp() {
        rule = new GeographyRule();

        alertRule = new AlertRule();
        alertRule.setRuleId(4);
        alertRule.setRuleCode("GEO-001");
        alertRule.setRuleName("High-Risk Jurisdiction");
        alertRule.setRuleCategory(RuleCategory.GEOGRAPHY);
        alertRule.setSeverity(AlertSeverity.HIGH);
        alertRule.setLookbackDays(30);
        alertRule.setIsActive(true);

        account = new Account();
        account.setAccountId(1);
        account.setAccountNumber("ACC-0004");
    }

    @ParameterizedTest
    @ValueSource(strings = {"IR", "KP", "SY", "CU", "MM", "BY", "RU", "VE"})
    void fires_for_each_default_high_risk_country(String countryCode) {
        RuleContext ctx = buildContext(countryCode);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Expected alert for high-risk country: " + countryCode);
    }

    @Test
    void fires_for_lowercase_high_risk_country() {
        RuleContext ctx = buildContext("ir");   // lowercase — should still match

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Country matching should be case-insensitive");
    }

    @Test
    void fires_for_mixed_case_high_risk_country() {
        RuleContext ctx = buildContext("Ru");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent(), "Country matching should be case-insensitive");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GB", "US", "DE", "FR", "JP", "AU", "CA", "NL"})
    void does_not_fire_for_low_risk_countries(String countryCode) {
        RuleContext ctx = buildContext(countryCode);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should NOT alert for safe country: " + countryCode);
    }


    @Test
    void returns_empty_when_country_is_null() {
        RuleContext ctx = buildContext(null);

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should return empty when counterpartyCountry is null");
    }

    @Test
    void returns_empty_when_country_is_blank() {
        RuleContext ctx = buildContext("   ");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should return empty when counterpartyCountry is blank");
    }

    @Test
    void returns_empty_when_country_is_empty_string() {
        RuleContext ctx = buildContext("");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent(), "Should return empty when counterpartyCountry is empty string");
    }

    // ── reason quality ───────────────────────────────────────────────────────

    @Test
    void reason_contains_country_code() {
        RuleContext ctx = buildContext("IR");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("IR"), "Reason should mention the offending country code");
    }

    @Test
    void reason_contains_txn_ref() {
        RuleContext ctx = buildContext("KP");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
        assertTrue(result.get().getReason().contains("TXN-GEO-001"), "Reason should contain txn reference");
    }

    @Test
    void supported_category_is_geography() {
        assertEquals("GEOGRAPHY", rule.getSupportedCategory());
    }

    @Test
    void supports_returns_true_for_geography_rule_code() {
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
    void supports_returns_false_for_non_geography_rule_code() {
        alertRule.setRuleCode("VEL-001");
        assertFalse(rule.supports(alertRule));
    }

    @Test
    void fires_for_trimmed_high_risk_country_from_custom_configuration() {
        rule.setHighRiskCountries(List.of(" ir ", " ru "));
        RuleContext ctx = buildContext("IR");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertTrue(result.isPresent());
    }

    @Test
    void does_not_fire_when_country_has_spaces_because_input_is_not_trimmed() {
        RuleContext ctx = buildContext(" IR ");

        Optional<RuleMatch> result = rule.evaluate(ctx, alertRule);

        assertFalse(result.isPresent());
    }

    // Helper methods

    private RuleContext buildContext(String counterpartyCountry) {
        Txn txn = new Txn();
        txn.setTxnId(1);
        txn.setTxnRef("TXN-GEO-001");
        txn.setAmountUsd(new BigDecimal("5000.00"));
        txn.setCounterpartyCountry(counterpartyCountry);

        return RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(List.of())
                .build();
    }
}
