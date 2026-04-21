package com.grad.sam.service.reports.workload;

import com.grad.sam.service.reports.GeneratedReportResult;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class InvestigatorWorkloadReportResult implements GeneratedReportResult {
    String reportName;
    Path reportPath;
    int recordCount;
    int analystCount;
    LocalDateTime generatedAt;
    LocalDateTime closedLast24HoursSince;
    LocalDateTime closedLast7DaysSince;
    String trigger;
    List<InvestigatorWorkloadReportRow> rows;
}
