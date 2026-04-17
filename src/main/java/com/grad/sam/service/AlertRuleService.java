package com.grad.sam.service;

import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.AlertRule;
import com.grad.sam.repository.AlertRuleRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;

    @Transactional
    public AlertRule saveRule(@NotNull AlertRule rule) {
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Saved alert rule: {} ({})", saved.getRuleCode(), saved.getRuleId());
        return saved;
    }

    @Transactional
    public void deactivate(@NotNull Integer ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new DataNotFoundException("Alert rule not found for id: " + ruleId));

        rule.setIsActive(false);
        alertRuleRepository.save(rule);
        log.info("Deactivated alert rule: {} ({})", rule.getRuleCode(), ruleId);
    }

    @Transactional
    public void deleteRule(@NotNull Integer ruleId) {
        alertRuleRepository.deleteById(ruleId);
        log.info("Deleted alert rule: {}", ruleId);
    }
}
