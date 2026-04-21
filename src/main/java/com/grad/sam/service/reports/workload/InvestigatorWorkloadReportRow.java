package com.grad.sam.service.reports.workload;

public record InvestigatorWorkloadReportRow(
        String analyst,
        int assignedCases,
        int openCases,
        int closedLast24Hours,
        int closedLast7Days,
        String averageClosureHours
) {
}
