package com.grad.sam.service.reports.ruleeffectiveness;

import com.grad.sam.service.reports.GeneratedReportResult;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class RuleEffectivenessReportResult implements GeneratedReportResult {
    String reportName;
    Path reportPath;
    int recordCount;
    int ruleCount;
    int actionableAlertCount;
    int alertsNeedingReviewCount;
    int falsePositiveCount;
    LocalDateTime generatedAt;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
    String trigger;
    List<RuleEffectivenessReportRow> rows;
}
