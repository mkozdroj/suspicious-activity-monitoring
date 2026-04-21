package com.grad.sam.service.reports.opencases;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Investigation;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenCasesReportService implements ReportGenerator<OpenCasesReportResult> {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<AlertStatus> REPORTABLE_STATUSES = List.of(
            AlertStatus.OPEN,
            AlertStatus.UNDER_REVIEW,
            AlertStatus.ESCALATED
    );
    private static final List<String> HEADERS = List.of(
            "alert_ref",
            "alert_status",
            "triggered_at",
            "rule_code",
            "rule_name",
            "account_number",
            "customer_ref",
            "full_name",
            "risk_rating",
            "investigation_ref",
            "priority",
            "opened_by",
            "opened_at"
    );

    private final AlertRepository alertRepository;
    private final ReportCsvWriter reportCsvWriter;

    @Value("${sam.reports.open-cases.output-dir:reports}")
    private String outputDir;

    @Value("${sam.reports.open-cases.file-prefix:open_cases_report}")
    private String filePrefix;

    @Transactional(readOnly = true)
    @Override
    public OpenCasesReportResult generateReport(String trigger) {
        List<Alert> alerts = alertRepository.findByStatusInOrderByTriggeredAtAscAlertIdAsc(REPORTABLE_STATUSES);
        LocalDateTime generatedAt = LocalDateTime.now();
        int openCount = countByStatus(alerts, AlertStatus.OPEN);
        int escalatedCount = countByStatus(alerts, AlertStatus.ESCALATED);
        int underReviewCount = countByStatus(alerts, AlertStatus.UNDER_REVIEW);
        LocalDateTime periodStart = alerts.isEmpty() ? null : alerts.getFirst().getTriggeredAt();
        LocalDateTime periodEnd = alerts.isEmpty() ? null : alerts.getLast().getTriggeredAt();
        List<OpenCasesCategorySummary> categorySummaries = summarizeCategories(alerts);

        try {
            Path reportDir = Path.of(outputDir).toAbsolutePath().normalize();
            String fileName = filePrefix + "_" + generatedAt.format(FILE_TIMESTAMP) + ".csv";
            Path reportPath = reportDir.resolve(fileName);
            List<OpenCasesReportRow> rows = alerts.stream()
                    .map(this::toRow)
                    .toList();

            reportCsvWriter.writeCsv(reportPath, HEADERS, rows, this::toColumns);

            log.info("Open cases report generated: path={}, rows={}, trigger={}",
                    reportPath,
                    alerts.size(),
                    trigger);

            return OpenCasesReportResult.builder()
                    .reportName(fileName)
                    .reportPath(reportPath)
                    .recordCount(alerts.size())
                    .openCount(openCount)
                    .escalatedCount(escalatedCount)
                    .underReviewCount(underReviewCount)
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .generatedAt(generatedAt)
                    .trigger(trigger)
                    .rows(rows)
                    .categorySummaries(categorySummaries)
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate open cases report", ex);
        }
    }

    private OpenCasesReportRow toRow(Alert alert) {
        Account account = alert.getAccount();
        Customer customer = account != null ? account.getCustomer() : null;
        Investigation investigation = alert.getInvestigation();

        return new OpenCasesReportRow(
                value(alert.getAlertRef()),
                value(alert.getStatus()),
                value(alert.getTriggeredAt()),
                value(alert.getAlertRule() != null ? alert.getAlertRule().getRuleCode() : null),
                value(alert.getAlertRule() != null ? alert.getAlertRule().getRuleName() : null),
                value(account != null ? account.getAccountNumber() : null),
                value(customer != null ? customer.getCustomerRef() : null),
                value(customer != null ? customer.getFullName() : null),
                value(customer != null ? customer.getRiskRating() : null),
                value(investigation != null ? investigation.getInvestigationRef() : null),
                value(investigation != null ? investigation.getPriority() : null),
                value(investigation != null ? investigation.getOpenedBy() : null),
                value(investigation != null ? investigation.getOpenedAt() : null)
        );
    }

    private List<String> toColumns(OpenCasesReportRow row) {
        return List.of(
                row.alertRef(),
                row.alertStatus(),
                row.triggeredAt(),
                row.ruleCode(),
                row.ruleName(),
                row.accountNumber(),
                row.customerRef(),
                row.fullName(),
                row.riskRating(),
                row.investigationRef(),
                row.priority(),
                row.openedBy(),
                row.openedAt()
        );
    }

    private int countByStatus(List<Alert> alerts, AlertStatus status) {
        return (int) alerts.stream()
                .filter(alert -> status.equals(alert.getStatus()))
                .count();
    }

    private List<OpenCasesCategorySummary> summarizeCategories(List<Alert> alerts) {
        return alerts.stream()
                .collect(Collectors.groupingBy(
                        alert -> value(alert.getAlertRule() != null ? alert.getAlertRule().getRuleCategory() : null),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new OpenCasesCategorySummary(
                        entry.getKey(),
                        entry.getValue()
                ))
                .sorted(Comparator
                        .comparingLong(OpenCasesCategorySummary::count).reversed()
                        .thenComparing(OpenCasesCategorySummary::category))
                .toList();
    }

    private String value(Object input) {
        return input == null ? "" : input.toString();
    }
}
