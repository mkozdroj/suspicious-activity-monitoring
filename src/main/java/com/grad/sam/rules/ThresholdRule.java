package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ThresholdRule implements AmlRule {

    @Override
    public String getSupportedCategory() {
        return "STRUCTURING";
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        if (rule.getThresholdAmount() == null) {
            return Optional.empty();
        }

        BigDecimal amountUsd = context.getTxn().getAmountUsd();

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