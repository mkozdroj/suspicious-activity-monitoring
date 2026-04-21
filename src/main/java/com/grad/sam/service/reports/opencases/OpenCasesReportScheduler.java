package com.grad.sam.service.reports.opencases;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "sam.reports.open-cases.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OpenCasesReportScheduler {

    private final OpenCasesReportEmailService openCasesReportEmailService;
    private final OpenCasesReportService openCasesReportService;

    @Scheduled(
            fixedDelayString = "${sam.reports.open-cases.scheduler.fixed-delay-ms:86400000}",
            initialDelayString = "${sam.reports.open-cases.scheduler.initial-delay-ms:60000}"
    )
    public void generateScheduledReport() {
        log.info("Scheduled open cases report tick started");
        OpenCasesReportResult reportResult = openCasesReportService.generateReport("scheduler");
        try {
            openCasesReportEmailService.sendReportEmail(reportResult);
        } catch (IllegalStateException ex) {
            log.warn("Open cases report email skipped: {}", ex.getMessage());
        }
        log.info("Scheduled open cases report tick finished");
    }
}
