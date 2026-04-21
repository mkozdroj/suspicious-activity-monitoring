package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.service.reports.ruleeffectiveness.RuleEffectivenessReportEmailService;
import com.grad.sam.service.reports.ruleeffectiveness.RuleEffectivenessReportResult;
import com.grad.sam.service.reports.ruleeffectiveness.RuleEffectivenessReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Validated
@RequiredArgsConstructor
public class RuleEffectivenessReportController {

    private final RuleEffectivenessReportService ruleEffectivenessReportService;
    private final RuleEffectivenessReportEmailService ruleEffectivenessReportEmailService;
    private final ReportResponseMapper reportResponseMapper;

    @PostMapping("/rule-effectiveness")
    public ResponseEntity<OpenCasesReportResponseDto> generateRuleEffectivenessReport() {
        try {
            RuleEffectivenessReportResult reportResult = ruleEffectivenessReportService.generateReport("manual");
            return ResponseEntity.ok(reportResponseMapper.toResponse(reportResult));
        } catch (IllegalStateException ex) {
            throw new BusinessConflictException("Failed to generate rule effectiveness report");
        }
    }

    @PostMapping("/rule-effectiveness/email")
    public ResponseEntity<OpenCasesReportResponseDto> generateAndEmailRuleEffectivenessReport() {
        try {
            RuleEffectivenessReportResult reportResult = ruleEffectivenessReportService.generateReport("manual-email");
            return ResponseEntity.ok(reportResponseMapper.toResponse(
                    reportResult,
                    true,
                    ruleEffectivenessReportEmailService.sendReportEmail(reportResult)
            ));
        } catch (IllegalStateException ex) {
            throw new BusinessConflictException("Failed to send rule effectiveness report email");
        }
    }
}
