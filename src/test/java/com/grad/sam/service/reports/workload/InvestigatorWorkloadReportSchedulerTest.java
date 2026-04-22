package com.grad.sam.service.reports.workload;

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
class InvestigatorWorkloadReportSchedulerTest {

    @Mock
    private InvestigatorWorkloadReportEmailService emailService;

    @Mock
    private InvestigatorWorkloadReportService reportService;

    @Test
    void generateScheduledReport_generates_and_sends_email() {
        InvestigatorWorkloadReportResult result = InvestigatorWorkloadReportResult.builder()
                .reportName("workload.csv")
                .reportPath(Path.of("/tmp/workload.csv"))
                .recordCount(1)
                .analystCount(1)
                .generatedAt(LocalDateTime.now())
                .trigger("scheduler")
                .rows(List.of())
                .build();
        when(reportService.generateReport("scheduler")).thenReturn(result);

        new InvestigatorWorkloadReportScheduler(emailService, reportService).generateScheduledReport();

        verify(reportService).generateReport("scheduler");
        verify(emailService).sendReportEmail(result);
    }

    @Test
    void generateScheduledReport_swallows_email_exception() {
        InvestigatorWorkloadReportResult result = InvestigatorWorkloadReportResult.builder()
                .reportName("workload.csv")
                .reportPath(Path.of("/tmp/workload.csv"))
                .recordCount(1)
                .analystCount(1)
                .generatedAt(LocalDateTime.now())
                .trigger("scheduler")
                .rows(List.of())
                .build();
        when(reportService.generateReport("scheduler")).thenReturn(result);
        doThrow(new IllegalStateException("disabled")).when(emailService).sendReportEmail(result);

        new InvestigatorWorkloadReportScheduler(emailService, reportService).generateScheduledReport();

        verify(reportService).generateReport("scheduler");
        verify(emailService).sendReportEmail(result);
    }
}
