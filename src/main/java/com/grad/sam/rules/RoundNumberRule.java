package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class RoundNumberRule implements AmlRule {

    private static final BigDecimal DEFAULT_DIVISOR = new BigDecimal("1000");

    @Override
    public String getSupportedCategory() {
        return "PATTERN";
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        BigDecimal amountUsd = context.getTxn().getAmountUsd();

        BigDecimal divisor = (rule.getThresholdAmount() != null && rule.getThresholdAmount().compareTo(BigDecimal.ZERO) > 0)
                ? rule.getThresholdAmount()
                : DEFAULT_DIVISOR;

        BigDecimal remainder = amountUsd.remainder(divisor).abs().setScale(2, RoundingMode.HALF_UP);

        if (remainder.compareTo(BigDecimal.ZERO) == 0 && amountUsd.compareTo(divisor) >= 0) {
            String reason = String.format(
                    "Round-number transaction: USD %.2f is exactly divisible by %.0f — possible layering indicator",
                    amountUsd, divisor);
            return Optional.of(new RuleMatch(rule, reason));
        }

        return Optional.empty();
    }
}