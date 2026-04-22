package com.grad.sam.service.reports.workload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "sam.reports.investigator-workload.scheduler.enabled",
        havingValue = "true"
)
public class InvestigatorWorkloadReportScheduler {

    private final InvestigatorWorkloadReportEmailService investigatorWorkloadReportEmailService;
    private final InvestigatorWorkloadReportService investigatorWorkloadReportService;

    @Scheduled(
            cron = "${sam.reports.investigator-workload.scheduler.cron:0 0 8 * * MON}",
            zone = "${sam.reports.investigator-workload.scheduler.zone:Europe/Warsaw}"
    )
    public void generateScheduledReport() {
        log.info("Scheduled investigator workload report tick started");
        InvestigatorWorkloadReportResult reportResult = investigatorWorkloadReportService.generateReport("scheduler");
        try {
            investigatorWorkloadReportEmailService.sendReportEmail(reportResult);
        } catch (IllegalStateException ex) {
            log.warn("Investigator workload report email skipped: {}", ex.getMessage());
        }
        log.info("Scheduled investigator workload report tick finished");
    }
}
