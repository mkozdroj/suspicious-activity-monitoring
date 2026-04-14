package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import lombok.Value;

@Value
public class RuleMatch {
    AlertRule rule;
    String reason;
}