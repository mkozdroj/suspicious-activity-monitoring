package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PatternRule implements AmlRule {

    private static final int DEFAULT_REPEAT_THRESHOLD = 2;

    @Override
    public String getSupportedCategory() {
        return "PATTERN";
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

        int repeatThreshold = rule.getThresholdCount() != null
                ? rule.getThresholdCount()
                : DEFAULT_REPEAT_THRESHOLD;

        BigDecimal currentAmount = context.getTxn().getAmountUsd();
        if (currentAmount == null) {
            throw new IllegalStateException("Transaction amountUsd must not be null.");
        }

        Map<BigDecimal, Long> frequencyMap = Stream.concat(
                        context.getRecentTxns().stream().map(Txn::getAmountUsd),
                        Stream.of(currentAmount))
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));

        return frequencyMap.entrySet().stream()
                .filter(e -> e.getValue() >= repeatThreshold)
                .findFirst()
                .map(e -> {
                    String reason = String.format(
                            "Smurfing/pattern detected: amount USD %.2f repeated %d times in %d-day window",
                            e.getKey(), e.getValue(), rule.getLookbackDays());
                    return new RuleMatch(rule, reason);
                });
    }
}