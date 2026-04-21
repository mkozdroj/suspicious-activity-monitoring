package com.grad.sam.service.reports.opencases;

public record OpenCasesReportRow(
        String alertRef,
        String alertStatus,
        String triggeredAt,
        String ruleCode,
        String ruleName,
        String accountNumber,
        String customerRef,
        String fullName,
        String riskRating,
        String investigationRef,
        String priority,
        String openedBy,
        String openedAt
) {
}
