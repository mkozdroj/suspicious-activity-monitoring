package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Setter
@Component
@ConfigurationProperties(prefix = "sam.rules.geography")
public class GeographyRule implements AmlRule {

    private List<String> highRiskCountries = List.of("IR", "KP", "SY", "CU", "MM", "BY", "RU", "VE");

    @Override
    public String getSupportedCategory() {
        return "GEOGRAPHY";
    }

    @Override
    public boolean supports(AlertRule rule) {
        return rule != null
                && rule.getRuleCode() != null
                && rule.getRuleCode().startsWith("GEO-");
    }

    @Override
    public List<String> getSupportedRuleCodes() {
        return List.of("GEO-001", "GEO-002", "GEO-003", "GEO-004");
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        if (context == null) {
            throw new IllegalArgumentException("Rule context must not be null.");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule must not be null.");
        }
        if (context.getTxn() == null) {
            throw new IllegalArgumentException("Transaction in context must not be null.");
        }
        if (highRiskCountries == null) {
            throw new IllegalStateException("High-risk countries configuration must not be null.");
        }

        return switch (rule.getRuleCode()) {
            case "GEO-001", "GEO-002" -> evaluateHighRiskCountry(context, rule);
            case "GEO-003" -> evaluateOffshoreJurisdiction(context, rule);
            case "GEO-004" -> evaluateHighRiskCorridor(context, rule);
            default -> Optional.empty();
        };
    }

    private Optional<RuleMatch> evaluateHighRiskCountry(RuleContext context, AlertRule rule) {
        String country = context.getTxn().getCounterpartyCountry();
        if (country == null || country.isBlank()) {
            return Optional.empty();
        }

        Set<String> upperCaseList = normalize(highRiskCountries);
        if (!upperCaseList.contains(country.toUpperCase())) {
            return Optional.empty();
        }

        String reason = String.format(
                "Geographic anomaly: transaction involves high-risk jurisdiction '%s' (txn_ref: %s)",
                country, context.getTxn().getTxnRef());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateOffshoreJurisdiction(RuleContext context, AlertRule rule) {
        String country = context.getTxn().getCounterpartyCountry();
        if (country == null || country.isBlank()) {
            return Optional.empty();
        }

        Set<String> offshoreCountries = Set.of("KY", "BVI", "PA");
        if (!offshoreCountries.contains(country.toUpperCase())) {
            return Optional.empty();
        }

        String reason = String.format(
                "Offshore shell jurisdiction detected: counterparty country '%s' matched offshore list",
                country);
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateHighRiskCorridor(RuleContext context, AlertRule rule) {
        Set<String> corridorCountries = Set.of("MX", "US", "KY");
        Set<String> countriesInWindow = Stream.concat(
                        context.getRecentTxns().stream().map(txn -> txn.getCounterpartyCountry()),
                        Stream.of(context.getTxn().getCounterpartyCountry()))
                .filter(country -> country != null && !country.isBlank())
                .map(String::toUpperCase)
                .filter(corridorCountries::contains)
                .collect(java.util.stream.Collectors.toSet());

        if (countriesInWindow.size() < 2) {
            return Optional.empty();
        }

        String reason = String.format(
                "High-risk corridor detected: transaction window spans corridor countries %s",
                countriesInWindow);
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Set<String> normalize(List<String> countries) {
        Set<String> upperCaseList = new HashSet<>();
        for (String c : countries) {
            upperCaseList.add(c.trim().toUpperCase());
        }
        return upperCaseList;
    }
}