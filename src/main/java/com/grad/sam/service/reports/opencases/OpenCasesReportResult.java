package com.grad.sam.service.reports.opencases;

import com.grad.sam.service.reports.GeneratedReportResult;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OpenCasesReportResult implements GeneratedReportResult {
    String reportName;
    Path reportPath;
    int recordCount;
    int openCount;
    int escalatedCount;
    int underReviewCount;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
    LocalDateTime generatedAt;
    String trigger;
    List<OpenCasesReportRow> rows;
    List<OpenCasesCategorySummary> categorySummaries;
}
