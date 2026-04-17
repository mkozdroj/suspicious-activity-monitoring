package com.grad.sam.repository;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Integer> {

    Optional<AlertRule> findByRuleId(Integer ruleId);

    Optional<AlertRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);

    List<AlertRule> findByIsActiveTrue();

    List<AlertRule> findByIsActiveFalse();

    List<AlertRule> findByRuleCategory(RuleCategory ruleCategory);

    List<AlertRule> findByRuleCategoryAndIsActiveTrue(RuleCategory ruleCategory);

    List<AlertRule> findBySeverityAndIsActiveTrue(AlertSeverity severity);

    List<AlertRule> findByThresholdAmountLessThanEqualAndIsActiveTrue(BigDecimal amount);

    List<AlertRule> findByThresholdCountLessThanEqualAndIsActiveTrue(Integer count);

    default AlertRule saveRule(AlertRule rule) {
        Logger log = LoggerFactory.getLogger(AlertRuleRepository.class);
        AlertRule saved = save(rule);
        log.info("Saved alert rule: {} ({})", saved.getRuleCode(), saved.getRuleId());
        return saved;
    }

    default void deactivate(Integer ruleId) {
        Logger log = LoggerFactory.getLogger(AlertRuleRepository.class);
        findById(ruleId).ifPresentOrElse(rule -> {
            rule.setIsActive(false);
            save(rule);
            log.info("Deactivated alert rule: {} ({})", rule.getRuleCode(), ruleId);
        }, () -> log.warn("Cannot deactivate — rule not found: {}", ruleId));
    }

    default void deleteRule(Integer ruleId) {
        Logger log = LoggerFactory.getLogger(AlertRuleRepository.class);
        deleteById(ruleId);
        log.info("Deleted alert rule: {}", ruleId);
    }

}