package com.grad.sam.service.reports.workload;

import com.grad.sam.enums.InvestigationState;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.InvestigationRepository;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestigatorWorkloadReportService implements ReportGenerator<InvestigatorWorkloadReportResult> {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<String> HEADERS = List.of(
            "analyst",
            "assigned_cases",
            "open_cases",
            "closed_last_24_hours",
            "closed_last_7_days",
            "average_closure_hours"
    );
    private static final DecimalFormat HOURS_FORMAT = new DecimalFormat("0.00");

    private final InvestigationRepository investigationRepository;
    private final ReportCsvWriter reportCsvWriter;

    @Value("${sam.reports.investigator-workload.output-dir:reports}")
    private String outputDir;

    @Value("${sam.reports.investigator-workload.file-prefix:investigator_workload_report}")
    private String filePrefix;

    @Transactional(readOnly = true)
    @Override
    public InvestigatorWorkloadReportResult generateReport(String trigger) {
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime closedLast24HoursSince = generatedAt.minusHours(24);
        LocalDateTime closedLast7DaysSince = generatedAt.minusDays(7);

        List<Investigation> investigations = investigationRepository.findAll();
        List<InvestigatorWorkloadReportRow> rows = investigations.stream()
                .collect(Collectors.groupingBy(Investigation::getOpenedBy))
                .entrySet()
                .stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue(), closedLast24HoursSince, closedLast7DaysSince))
                .sorted(Comparator
                        .comparingInt(InvestigatorWorkloadReportRow::assignedCases).reversed()
                        .thenComparing(InvestigatorWorkloadReportRow::analyst))
                .toList();

        try {
            Path reportDir = Path.of(outputDir).toAbsolutePath().normalize();
            String fileName = filePrefix + "_" + generatedAt.format(FILE_TIMESTAMP) + ".csv";
            Path reportPath = reportDir.resolve(fileName);

            reportCsvWriter.writeCsv(reportPath, HEADERS, rows, this::toColumns);

            log.info("Investigator workload report generated: path={}, analysts={}, trigger={}",
                    reportPath,
                    rows.size(),
                    trigger);

            return InvestigatorWorkloadReportResult.builder()
                    .reportName(fileName)
                    .reportPath(reportPath)
                    .recordCount(investigations.size())
                    .analystCount(rows.size())
                    .generatedAt(generatedAt)
                    .closedLast24HoursSince(closedLast24HoursSince)
                    .closedLast7DaysSince(closedLast7DaysSince)
                    .trigger(trigger)
                    .rows(rows)
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate investigator workload report", ex);
        }
    }

    private InvestigatorWorkloadReportRow toRow(String analyst,
                                                List<Investigation> investigations,
                                                LocalDateTime closedLast24HoursSince,
                                                LocalDateTime closedLast7DaysSince) {
        int assignedCases = investigations.size();
        int openCases = (int) investigations.stream()
                .filter(investigation -> investigation.getState() != InvestigationState.CLOSED)
                .count();
        int closedLast24Hours = (int) investigations.stream()
                .filter(investigation -> wasClosedSince(investigation, closedLast24HoursSince))
                .count();
        int closedLast7Days = (int) investigations.stream()
                .filter(investigation -> wasClosedSince(investigation, closedLast7DaysSince))
                .count();
        String averageClosureHours = formatAverageClosureHours(investigations);

        return new InvestigatorWorkloadReportRow(
                analyst == null || analyst.isBlank() ? "UNASSIGNED" : analyst,
                assignedCases,
                openCases,
                closedLast24Hours,
                closedLast7Days,
                averageClosureHours
        );
    }

    private boolean wasClosedSince(Investigation investigation, LocalDateTime since) {
        return investigation.getClosedAt() != null && !investigation.getClosedAt().isBefore(since);
    }

    private String formatAverageClosureHours(List<Investigation> investigations) {
        double averageHours = investigations.stream()
                .filter(investigation -> investigation.getOpenedAt() != null && investigation.getClosedAt() != null)
                .mapToLong(investigation -> Duration.between(investigation.getOpenedAt(), investigation.getClosedAt()).toMinutes())
                .average()
                .orElse(Double.NaN);

        if (Double.isNaN(averageHours)) {
            return "";
        }

        return HOURS_FORMAT.format(averageHours / 60.0d);
    }

    private List<String> toColumns(InvestigatorWorkloadReportRow row) {
        return List.of(
                row.analyst(),
                Integer.toString(row.assignedCases()),
                Integer.toString(row.openCases()),
                Integer.toString(row.closedLast24Hours()),
                Integer.toString(row.closedLast7Days()),
                row.averageClosureHours()
        );
    }

}
