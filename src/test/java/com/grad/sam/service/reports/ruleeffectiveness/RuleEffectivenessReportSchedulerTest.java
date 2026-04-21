package com.grad.sam.service.reports.ruleeffectiveness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEffectivenessReportSchedulerTest {

    @Mock
    private RuleEffectivenessReportEmailService emailService;

    @Mock
    private RuleEffectivenessReportService reportService;

    @Test
    void generateScheduledReport_generates_and_sends_email() {
        RuleEffectivenessReportResult result = RuleEffectivenessReportResult.builder()
                .reportName("rules.csv")
                .reportPath(Path.of("/tmp/rules.csv"))
                .recordCount(1)
                .ruleCount(1)
                .generatedAt(LocalDateTime.now())
                .trigger("scheduler")
                .rows(List.of())
                .build();
        when(reportService.generateReport("scheduler")).thenReturn(result);

        new RuleEffectivenessReportScheduler(emailService, reportService).generateScheduledReport();

        verify(reportService).generateReport("scheduler");
        verify(emailService).sendReportEmail(result);
    }

    @Test
    void generateScheduledReport_swallows_email_exception() {
        RuleEffectivenessReportResult result = RuleEffectivenessReportResult.builder()
                .reportName("rules.csv")
                .reportPath(Path.of("/tmp/rules.csv"))
                .recordCount(1)
                .ruleCount(1)
                .generatedAt(LocalDateTime.now())
                .trigger("scheduler")
                .rows(List.of())
                .build();
        when(reportService.generateReport("scheduler")).thenReturn(result);
        doThrow(new IllegalStateException("disabled")).when(emailService).sendReportEmail(result);

        new RuleEffectivenessReportScheduler(emailService, reportService).generateScheduledReport();

        verify(reportService).generateReport("scheduler");
        verify(emailService).sendReportEmail(result);
    }
}
