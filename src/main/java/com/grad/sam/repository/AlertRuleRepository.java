package com.grad.sam.repository;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.AlertRule;
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

    // severity is AlertSeverity enum not String
    List<AlertRule> findBySeverityAndIsActiveTrue(AlertSeverity severity);

    List<AlertRule> findByThresholdAmountLessThanEqualAndIsActiveTrue(BigDecimal amount);

    List<AlertRule> findByThresholdCountLessThanEqualAndIsActiveTrue(Integer count);
}