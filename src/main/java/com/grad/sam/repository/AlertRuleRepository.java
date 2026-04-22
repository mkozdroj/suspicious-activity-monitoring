package com.grad.sam.repository;

import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Integer> {

    List<AlertRule> findByIsActiveTrue();

    List<AlertRule> findByRuleCategoryAndIsActiveTrue(RuleCategory ruleCategory);
}
