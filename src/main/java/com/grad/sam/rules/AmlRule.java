package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;

import java.util.List;
import java.util.Optional;

public interface AmlRule {
    String getSupportedCategory();

    boolean supports(AlertRule rule);

    List<String> getSupportedRuleCodes();

    Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule);
}
