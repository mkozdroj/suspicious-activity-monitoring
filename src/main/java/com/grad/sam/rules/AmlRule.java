package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import java.util.Optional;

public interface AmlRule {
    String getSupportedCategory();
    boolean supports(AlertRule rule);
    Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule);
}
