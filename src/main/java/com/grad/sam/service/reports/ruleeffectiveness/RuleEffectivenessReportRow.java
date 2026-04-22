package com.grad.sam.service.reports.ruleeffectiveness;

public record RuleEffectivenessReportRow(
        String ruleCode,
        String ruleName,
        String category,
        int totalAlerts,
        int alertsNeedingReview,
        int actionableAlerts,
        int escalatedAlerts,
        int sarFiledAlerts,
        int clearedAlerts,
        int falsePositiveAlerts,
        String averageAlertScore,
        String actionableRatePercent
) {
}
