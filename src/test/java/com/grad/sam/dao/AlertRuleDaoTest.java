package com.grad.sam.dao;

import com.grad.sam.model.AlertRule;
import com.grad.sam.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AlertRuleDaoTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    private AlertRuleDao alertRuleDao;

    @BeforeEach
    void setUp() {
        alertRuleDao = new AlertRuleDao(alertRuleRepository);
    }

    @Test
    void findActiveRules_returns_repository_result() {
        AlertRule rule1 = new AlertRule();
        AlertRule rule2 = new AlertRule();
        List<AlertRule> expected = List.of(rule1, rule2);

        when(alertRuleRepository.findByIsActiveTrue()).thenReturn(expected);

        List<AlertRule> result = alertRuleDao.findActiveRules();

        assertEquals(expected, result);
        verify(alertRuleRepository).findByIsActiveTrue();
    }

    @Test
    void findAllRules_returns_repository_result() {
        AlertRule rule1 = new AlertRule();
        AlertRule rule2 = new AlertRule();
        List<AlertRule> expected = List.of(rule1, rule2);

        when(alertRuleRepository.findAll()).thenReturn(expected);

        List<AlertRule> result = alertRuleDao.findAllRules();

        assertEquals(expected, result);
        verify(alertRuleRepository).findAll();
    }

    @Test
    void findById_returns_repository_result() {
        AlertRule rule = new AlertRule();
        rule.setRuleId(1);

        when(alertRuleRepository.findById(1)).thenReturn(Optional.of(rule));

        Optional<AlertRule> result = alertRuleDao.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getRuleId());
        verify(alertRuleRepository).findById(1);
    }

    @Test
    void save_returns_saved_rule() {
        AlertRule rule = new AlertRule();
        rule.setRuleId(1);
        rule.setRuleCode("THR-001");

        when(alertRuleRepository.save(rule)).thenReturn(rule);

        AlertRule result = alertRuleDao.save(rule);

        assertNotNull(result);
        assertEquals(1, result.getRuleId());
        assertEquals("THR-001", result.getRuleCode());
        verify(alertRuleRepository).save(rule);
    }

    @Test
    void deactivate_sets_rule_inactive_and_saves_when_rule_exists() {
        AlertRule rule = new AlertRule();
        rule.setRuleId(1);
        rule.setRuleCode("THR-001");
        rule.setIsActive(true);

        when(alertRuleRepository.findById(1)).thenReturn(Optional.of(rule));

        alertRuleDao.deactivate(1);

        assertFalse(rule.getIsActive());
        verify(alertRuleRepository).findById(1);
        verify(alertRuleRepository).save(rule);
    }

    @Test
    void deactivate_does_not_save_when_rule_not_found() {
        when(alertRuleRepository.findById(1)).thenReturn(Optional.empty());

        alertRuleDao.deactivate(1);

        verify(alertRuleRepository).findById(1);
        verify(alertRuleRepository, never()).save(any(AlertRule.class));
    }

    @Test
    void delete_calls_repository_deleteById() {
        alertRuleDao.delete(1);

        verify(alertRuleRepository).deleteById(1);
    }
}