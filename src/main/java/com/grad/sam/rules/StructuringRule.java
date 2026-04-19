package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class StructuringRule implements AmlRule {

    private static final BigDecimal LOWER_BAND_FACTOR = new BigDecimal("0.75");

    @Override
    public String getSupportedCategory() {
        return "STRUCTURING";
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
        if (context.getRecentTxns() == null) {
            throw new IllegalStateException("Recent transactions list must not be null.");
        }

        if (rule.getThresholdAmount() == null) {
            return Optional.empty();
        }

        BigDecimal threshold = rule.getThresholdAmount();
        BigDecimal lowerBand = threshold.multiply(LOWER_BAND_FACTOR);

        BigDecimal windowTotal = context.getRecentTxns().stream()
                .map(Txn::getAmountUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentAmount = context.getTxn().getAmountUsd();
        if (currentAmount == null) {
            throw new IllegalStateException("Transaction amountUsd must not be null.");
        }

        windowTotal = windowTotal.add(currentAmount);

        boolean isStructuring = windowTotal.compareTo(lowerBand) >= 0
                && windowTotal.compareTo(threshold) < 0;

        if (isStructuring) {
            String reason = String.format(
                    "Structuring detected: cumulative USD %.2f in %d-day window approaches threshold USD %.2f",
                    windowTotal, rule.getLookbackDays(), threshold);
            return Optional.of(new RuleMatch(rule, reason));
        }

        return Optional.empty();
    }
}