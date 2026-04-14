package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "sam.rules.geography")
public class GeographyRule implements AmlRule {

    private List<String> highRiskCountries = List.of("IR", "KP", "SY", "CU", "MM", "BY", "RU", "VE");

    // Setter required for @ConfigurationProperties binding
    public void setHighRiskCountries(List<String> highRiskCountries) {
        this.highRiskCountries = highRiskCountries;
    }

    @Override
    public String getSupportedCategory() {
        return "GEOGRAPHY";
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        String country = context.getTxn().getCounterpartyCountry();

        if (country == null || country.isBlank()) {
            return Optional.empty();
        }

        Set<String> upperCaseList = new HashSet<>();
        for (String c : highRiskCountries) {
            upperCaseList.add(c.trim().toUpperCase());
        }

        if (upperCaseList.contains(country.toUpperCase())) {
            String reason = String.format(
                    "Geographic anomaly: transaction involves high-risk jurisdiction '%s' (txn_ref: %s)",
                    country, context.getTxn().getTxnRef());
            return Optional.of(new RuleMatch(rule, reason));
        }

        return Optional.empty();
    }
}