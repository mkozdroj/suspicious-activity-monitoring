package com.grad.sam.service.reports.ruleeffectiveness;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.model.Alert;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.service.reports.ReportCsvWriter;
import com.grad.sam.service.reports.ReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEffectivenessReportService implements ReportGenerator<RuleEffectivenessReportResult> {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<String> HEADERS = List.of(
            "rule_code",
            "rule_name",
            "category",
            "total_alerts",
            "alerts_needing_review",
            "actionable_alerts",
            "escalated_alerts",
            "sar_filed_alerts",
            "cleared_alerts",
            "false_positive_alerts",
            "average_alert_score",
            "actionable_rate_percent"
    );
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final AlertRepository alertRepository;
    private final ReportCsvWriter reportCsvWriter;

    @Value("${sam.reports.rule-effectiveness.output-dir:reports}")
    private String outputDir;

    @Value("${sam.reports.rule-effectiveness.file-prefix:rule_effectiveness_report}")
    private String filePrefix;

    @Transactional(readOnly = true)
    @Override
    public RuleEffectivenessReportResult generateReport(String trigger) {
        List<Alert> alerts = alertRepository.findAllByOrderByTriggeredAtAscAlertIdAsc();
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime periodStart = alerts.isEmpty() ? null : alerts.getFirst().getTriggeredAt();
        LocalDateTime periodEnd = alerts.isEmpty() ? null : alerts.getLast().getTriggeredAt();

        List<RuleEffectivenessReportRow> rows = alerts.stream()
                .collect(Collectors.groupingBy(alert -> new RuleKey(
                        value(alert.getAlertRule() != null ? alert.getAlertRule().getRuleCode() : null),
                        value(alert.getAlertRule() != null ? alert.getAlertRule().getRuleName() : null),
                        value(alert.getAlertRule() != null ? alert.getAlertRule().getRuleCategory() : null)
                )))
                .entrySet()
                .stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt(RuleEffectivenessReportRow::totalAlerts).reversed()
                        .thenComparing(RuleEffectivenessReportRow::ruleCode))
                .toList();
        int actionableAlertCount = rows.stream().mapToInt(RuleEffectivenessReportRow::actionableAlerts).sum();
        int alertsNeedingReviewCount = rows.stream().mapToInt(RuleEffectivenessReportRow::alertsNeedingReview).sum();
        int falsePositiveCount = rows.stream().mapToInt(RuleEffectivenessReportRow::falsePositiveAlerts).sum();

        try {
            Path reportDir = Path.of(outputDir).toAbsolutePath().normalize();
            String fileName = filePrefix + "_" + generatedAt.format(FILE_TIMESTAMP) + ".csv";
            Path reportPath = reportDir.resolve(fileName);

            reportCsvWriter.writeCsv(reportPath, HEADERS, rows, this::toColumns);

            log.info("Rule effectiveness report generated: path={}, rules={}, trigger={}",
                    reportPath,
                    rows.size(),
                    trigger);

            return RuleEffectivenessReportResult.builder()
                    .reportName(fileName)
                    .reportPath(reportPath)
                    .recordCount(alerts.size())
                    .ruleCount(rows.size())
                    .actionableAlertCount(actionableAlertCount)
                    .alertsNeedingReviewCount(alertsNeedingReviewCount)
                    .falsePositiveCount(falsePositiveCount)
                    .generatedAt(generatedAt)
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .trigger(trigger)
                    .rows(rows)
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate rule effectiveness report", ex);
        }
    }

    private RuleEffectivenessReportRow toRow(RuleKey key, List<Alert> alerts) {
        int totalAlerts = alerts.size();
        int alertsNeedingReview = (int) alerts.stream()
                .filter(alert -> alert.getStatus() == AlertStatus.OPEN || alert.getStatus() == AlertStatus.UNDER_REVIEW)
                .count();
        int escalatedAlerts = countByStatus(alerts, AlertStatus.ESCALATED);
        int sarFiledAlerts = countByStatus(alerts, AlertStatus.SAR_FILED);
        int actionableAlerts = escalatedAlerts + sarFiledAlerts;
        int clearedAlerts = countByStatus(alerts, AlertStatus.CLOSED);
        int falsePositiveAlerts = countByStatus(alerts, AlertStatus.FALSE_POSITIVE);

        double averageScore = alerts.stream()
                .map(Alert::getAlertScore)
                .filter(score -> score != null)
                .mapToInt(Short::intValue)
                .average()
                .orElse(Double.NaN);

        double actionableRate = totalAlerts == 0
                ? Double.NaN
                : ((double) actionableAlerts / totalAlerts) * 100.0d;

        return new RuleEffectivenessReportRow(
                key.ruleCode(),
                key.ruleName(),
                key.category(),
                totalAlerts,
                alertsNeedingReview,
                actionableAlerts,
                escalatedAlerts,
                sarFiledAlerts,
                clearedAlerts,
                falsePositiveAlerts,
                formatDecimal(averageScore),
                formatDecimal(actionableRate)
        );
    }

    private int countByStatus(List<Alert> alerts, AlertStatus status) {
        return (int) alerts.stream()
                .filter(alert -> status.equals(alert.getStatus()))
                .count();
    }

    private String formatDecimal(double value) {
        return Double.isNaN(value) ? "" : DECIMAL_FORMAT.format(value);
    }

    private List<String> toColumns(RuleEffectivenessReportRow row) {
        return List.of(
                row.ruleCode(),
                row.ruleName(),
                row.category(),
                Integer.toString(row.totalAlerts()),
                Integer.toString(row.alertsNeedingReview()),
                Integer.toString(row.actionableAlerts()),
                Integer.toString(row.escalatedAlerts()),
                Integer.toString(row.sarFiledAlerts()),
                Integer.toString(row.clearedAlerts()),
                Integer.toString(row.falsePositiveAlerts()),
                row.averageAlertScore(),
                row.actionableRatePercent()
        );
    }

    private String value(Object input) {
        return input == null ? "" : input.toString();
    }

    private record RuleKey(String ruleCode, String ruleName, String category) {
    }
}
