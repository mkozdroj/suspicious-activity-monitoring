package com.grad.sam.service;

import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.AlertRule;
import com.grad.sam.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    private AlertRuleService service;

    @BeforeEach
    void setUp() {
        service = new AlertRuleService(alertRuleRepository);
    }

    @Test
    void saveRule_persists_rule_via_repository() {
        AlertRule rule = buildRule(7, "VEL-001");
        when(alertRuleRepository.save(rule)).thenReturn(rule);

        AlertRule saved = service.saveRule(rule);

        assertSame(rule, saved);
        verify(alertRuleRepository).save(rule);
    }

    @Test
    void deactivate_marks_rule_inactive_and_saves() {
        AlertRule rule = buildRule(9, "GEO-001");
        when(alertRuleRepository.findById(9)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(rule)).thenReturn(rule);

        service.deactivate(9);

        assertFalse(rule.getIsActive());
        verify(alertRuleRepository).findById(9);
        verify(alertRuleRepository).save(rule);
    }

    @Test
    void deactivate_throws_when_rule_missing() {
        when(alertRuleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class, () -> service.deactivate(99));
    }

    @Test
    void deleteRule_deletes_by_id() {
        service.deleteRule(11);

        verify(alertRuleRepository).deleteById(11);
    }

    private AlertRule buildRule(Integer id, String code) {
        AlertRule rule = new AlertRule();
        rule.setRuleId(id);
        rule.setRuleCode(code);
        rule.setRuleName("Rule " + code);
        rule.setDescription("Test rule");
        rule.setIsActive(true);
        return rule;
    }
}
