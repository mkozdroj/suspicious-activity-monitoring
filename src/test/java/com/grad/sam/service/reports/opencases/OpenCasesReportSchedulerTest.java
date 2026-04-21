package com.grad.sam.service.reports.opencases;

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
class OpenCasesReportSchedulerTest {

    @Mock
    private OpenCasesReportEmailService emailService;

    @Mock
    private OpenCasesReportService reportService;

    @Test
    void generateScheduledReport_generates_and_sends_email() {
        OpenCasesReportResult result = OpenCasesReportResult.builder()
                .reportName("open.csv")
                .reportPath(Path.of("/tmp/open.csv"))
                .recordCount(1)
                .generatedAt(LocalDateTime.now())
                .trigger("scheduler")
                .rows(List.of())
                .categorySummaries(List.of())
                .build();
        when(reportService.generateReport("scheduler")).thenReturn(result);

        new OpenCasesReportScheduler(emailService, reportService).generateScheduledReport();

        verify(reportService).generateReport("scheduler");
        verify(emailService).sendReportEmail(result);
    }

    @Test
    void generateScheduledReport_swallow_email_exception() {
        OpenCasesReportResult result = OpenCasesReportResult.builder()
                .reportName("open.csv")
                .reportPath(Path.of("/tmp/open.csv"))
                .recordCount(1)
                .generatedAt(LocalDateTime.now())
                .trigger("scheduler")
                .rows(List.of())
                .categorySummaries(List.of())
                .build();
        when(reportService.generateReport("scheduler")).thenReturn(result);
        doThrow(new IllegalStateException("disabled")).when(emailService).sendReportEmail(result);

        new OpenCasesReportScheduler(emailService, reportService).generateScheduledReport();

        verify(reportService).generateReport("scheduler");
        verify(emailService).sendReportEmail(result);
    }
}
