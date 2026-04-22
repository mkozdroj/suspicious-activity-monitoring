package com.grad.sam.service.reports.ruleeffectiveness;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "sam.reports.rule-effectiveness.scheduler.enabled",
        havingValue = "true"
)
public class RuleEffectivenessReportScheduler {

    private final RuleEffectivenessReportEmailService ruleEffectivenessReportEmailService;
    private final RuleEffectivenessReportService ruleEffectivenessReportService;

    @Scheduled(
            cron = "${sam.reports.rule-effectiveness.scheduler.cron:0 30 8 * * MON}",
            zone = "${sam.reports.rule-effectiveness.scheduler.zone:Europe/Warsaw}"
    )
    public void generateScheduledReport() {
        log.info("Scheduled rule effectiveness report tick started");
        RuleEffectivenessReportResult reportResult = ruleEffectivenessReportService.generateReport("scheduler");
        try {
            ruleEffectivenessReportEmailService.sendReportEmail(reportResult);
        } catch (IllegalStateException ex) {
            log.warn("Rule effectiveness report email skipped: {}", ex.getMessage());
        }
        log.info("Scheduled rule effectiveness report tick finished");
    }
}
