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
        if (rule.getThresholdAmount() == null) {
            return Optional.empty();
        }

        BigDecimal threshold = rule.getThresholdAmount();
        BigDecimal lowerBand = threshold.multiply(LOWER_BAND_FACTOR);

        BigDecimal windowTotal = context.getRecentTxns().stream()
                .map(Txn::getAmountUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        windowTotal = windowTotal.add(context.getTxn().getAmountUsd());

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