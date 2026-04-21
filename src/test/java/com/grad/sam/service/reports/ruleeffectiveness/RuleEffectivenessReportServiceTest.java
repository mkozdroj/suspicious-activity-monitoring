package com.grad.sam.service.reports.ruleeffectiveness;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.model.Alert;
import com.grad.sam.model.AlertRule;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.service.reports.ReportCsvWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEffectivenessReportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private RuleEffectivenessReportService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new RuleEffectivenessReportService(alertRepository, new ReportCsvWriter());
        ReflectionTestUtils.setField(service, "outputDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "filePrefix", "rule_effectiveness_report");
    }

    @Test
    void generateReport_groups_by_rule_and_calculates_metrics() {
        Alert a1 = buildAlert("STR-001", "Structuring", RuleCategory.STRUCTURING, AlertStatus.OPEN, (short) 40, LocalDateTime.of(2026, 4, 20, 10, 0));
        Alert a2 = buildAlert("STR-001", "Structuring", RuleCategory.STRUCTURING, AlertStatus.ESCALATED, (short) 80, LocalDateTime.of(2026, 4, 20, 11, 0));
        Alert a3 = buildAlert("STR-001", "Structuring", RuleCategory.STRUCTURING, AlertStatus.FALSE_POSITIVE, (short) 60, LocalDateTime.of(2026, 4, 20, 12, 0));
        Alert a4 = buildAlert("GEO-001", "Geography", RuleCategory.GEOGRAPHY, AlertStatus.SAR_FILED, (short) 90, LocalDateTime.of(2026, 4, 20, 13, 0));

        when(alertRepository.findAllByOrderByTriggeredAtAscAlertIdAsc()).thenReturn(List.of(a1, a2, a3, a4));

        RuleEffectivenessReportResult result = service.generateReport("manual");

        assertEquals(4, result.getRecordCount());
        assertEquals(2, result.getRuleCount());
        assertEquals(2, result.getActionableAlertCount());
        assertEquals(1, result.getAlertsNeedingReviewCount());
        assertEquals(1, result.getFalsePositiveCount());
        assertEquals("STR-001", result.getRows().getFirst().ruleCode());
        assertEquals(3, result.getRows().getFirst().totalAlerts());
        assertEquals(1, result.getRows().getFirst().alertsNeedingReview());
        assertEquals(1, result.getRows().getFirst().actionableAlerts());
        assertEquals("60.00", result.getRows().getFirst().averageAlertScore().replace(',', '.'));
        assertEquals("33.33", result.getRows().getFirst().actionableRatePercent().replace(',', '.'));
        assertTrue(result.getReportPath().toFile().exists());
    }

    @Test
    void generateReport_wraps_csv_writer_failure() throws Exception {
        ReportCsvWriter writer = org.mockito.Mockito.mock(ReportCsvWriter.class);
        RuleEffectivenessReportService failingService = new RuleEffectivenessReportService(alertRepository, writer);
        ReflectionTestUtils.setField(failingService, "outputDir", tempDir.toString());
        ReflectionTestUtils.setField(failingService, "filePrefix", "rule_effectiveness_report");
        when(alertRepository.findAllByOrderByTriggeredAtAscAlertIdAsc()).thenReturn(List.of());
        doThrow(new IOException("disk full")).when(writer).writeCsv(any(), anyList(), anyList(), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> failingService.generateReport("manual"));

        assertTrue(exception.getMessage().contains("Failed to generate rule effectiveness report"));
    }

    private Alert buildAlert(String ruleCode,
                             String ruleName,
                             RuleCategory category,
                             AlertStatus status,
                             short score,
                             LocalDateTime triggeredAt) {
        AlertRule alertRule = new AlertRule();
        alertRule.setRuleCode(ruleCode);
        alertRule.setRuleName(ruleName);
        alertRule.setRuleCategory(category);
        alertRule.setSeverity(AlertSeverity.HIGH);

        Alert alert = new Alert();
        alert.setAlertRule(alertRule);
        alert.setStatus(status);
        alert.setAlertScore(score);
        alert.setTriggeredAt(triggeredAt);
        return alert;
    }
}
