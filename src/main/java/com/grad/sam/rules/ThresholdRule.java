package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class ThresholdRule implements AmlRule {

    @Override
    public String getSupportedCategory() {
        return "STRUCTURING";
    }

    @Override
    public List<String> getSupportedRuleCodes() {
        return List.of();
    }

    @Override
    public boolean supports(AlertRule rule) {
        return rule != null
                && rule.getRuleCode() != null
                && rule.getRuleCode().startsWith("THR-");
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
        if (context.getAccount() == null) {
            throw new IllegalArgumentException("Account in context must not be null.");
        }

        if (rule.getThresholdAmount() == null) {
            return Optional.empty();
        }

        BigDecimal amountUsd = context.getTxn().getAmountUsd();
        if (amountUsd == null) {
            throw new IllegalStateException("Transaction amountUsd must not be null.");
        }

        if (amountUsd.compareTo(rule.getThresholdAmount()) > 0) {
            String reason = String.format(
                    "Large transaction: %s %.2f (USD %.2f) exceeds threshold USD %.2f on account %s",
                    context.getTxn().getCurrency(),
                    context.getTxn().getAmount(),
                    amountUsd,
                    rule.getThresholdAmount(),
                    context.getAccount().getAccountNumber());
            return Optional.of(new RuleMatch(rule, reason));
        }

        return Optional.empty();
    }
}