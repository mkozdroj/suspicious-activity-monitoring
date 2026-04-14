package com.grad.sam.dao;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.model.AlertRule;
import com.grad.sam.repository.AlertRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AlertRuleDao {

    private final AlertRuleRepository alertRuleRepository;

    public AlertRuleDao(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    public List<AlertRule> findActiveRules() {
        return alertRuleRepository.findByIsActiveTrue();
    }

    public List<AlertRule> findAllRules() {
        return alertRuleRepository.findAll();
    }

    public Optional<AlertRule> findById(Integer ruleId) {
        return alertRuleRepository.findById(ruleId);
    }

    public AlertRule save(AlertRule rule) {
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Saved alert rule: {} ({})", saved.getRuleCode(), saved.getRuleId());
        return saved;
    }

    public void deactivate(Integer ruleId) {
        alertRuleRepository.findById(ruleId).ifPresentOrElse(rule -> {
            rule.setIsActive(false);
            alertRuleRepository.save(rule);
            log.info("Deactivated alert rule: {} ({})", rule.getRuleCode(), ruleId);
        }, () -> log.warn("Cannot deactivate — rule not found: {}", ruleId));
    }

    public void delete(Integer ruleId) {
        alertRuleRepository.deleteById(ruleId);
        log.info("Deleted alert rule: {}", ruleId);
    }
}