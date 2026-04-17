package com.grad.sam.repository;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.AlertRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
class AlertRuleRepositoryTest {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void deactivate_should_set_rule_inactive() {
        AlertRule rule = buildRule("VEL-001", "Velocity rule", RuleCategory.VELOCITY, AlertSeverity.HIGH);
        rule = em.persistFlushFind(rule);

        rule.setIsActive(false);
        alertRuleRepository.saveAndFlush(rule);
        em.flush();
        em.clear();

        AlertRule updated = em.find(AlertRule.class, rule.getRuleId());
        assertFalse(updated.getIsActive());
    }

    @Test
    void saveRule_should_persist_rule() {
        AlertRule rule = buildRule("GEO-001", "Geography rule", RuleCategory.GEOGRAPHY, AlertSeverity.MEDIUM);

        AlertRule saved = alertRuleRepository.saveAndFlush(rule);

        assertNotNull(saved.getRuleId());
        assertEquals("GEO-001", saved.getRuleCode());
    }

    @Test
    void deleteRule_should_remove_rule() {
        AlertRule rule = buildRule("PAT-001", "Pattern rule", RuleCategory.STRUCTURING, AlertSeverity.HIGH);
        rule = em.persistFlushFind(rule);

        Integer id = rule.getRuleId();

        alertRuleRepository.deleteById(id);
        em.flush();
        em.clear();

        assertFalse(alertRuleRepository.findById(id).isPresent());
    }

    private AlertRule buildRule(String code, String name, RuleCategory category, AlertSeverity severity) {
        AlertRule rule = new AlertRule();
        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setDescription("Test description for " + name);
        rule.setIsActive(true);
        rule.setRuleCategory(category);
        rule.setSeverity(severity);
        return rule;
    }
}
