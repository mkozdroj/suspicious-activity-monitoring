package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VelocityRule implements AmlRule {

    @Override
    public String getSupportedCategory() {
        return "VELOCITY";
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        if (rule.getThresholdCount() == null) {
            return Optional.empty();
        }

        // +1 to include the current transaction being screened
        int total = context.getRecentTxns().size() + 1;

        if (total > rule.getThresholdCount()) {
            String reason = String.format(
                    "Velocity breach: %d transactions in %d-day window (threshold: %d) on account %s",
                    total,
                    rule.getLookbackDays(),
                    rule.getThresholdCount(),
                    context.getAccount().getAccountNumber());
            return Optional.of(new RuleMatch(rule, reason));
        }

        return Optional.empty();
    }
}