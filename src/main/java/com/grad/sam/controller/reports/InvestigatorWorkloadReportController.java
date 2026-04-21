package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.service.reports.workload.InvestigatorWorkloadReportEmailService;
import com.grad.sam.service.reports.workload.InvestigatorWorkloadReportResult;
import com.grad.sam.service.reports.workload.InvestigatorWorkloadReportService;
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
public class InvestigatorWorkloadReportController {

    private final InvestigatorWorkloadReportService investigatorWorkloadReportService;
    private final InvestigatorWorkloadReportEmailService investigatorWorkloadReportEmailService;
    private final ReportResponseMapper reportResponseMapper;

    @PostMapping("/investigator-workload")
    public ResponseEntity<OpenCasesReportResponseDto> generateInvestigatorWorkloadReport() {
        try {
            InvestigatorWorkloadReportResult reportResult = investigatorWorkloadReportService.generateReport("manual");
            return ResponseEntity.ok(reportResponseMapper.toResponse(reportResult));
        } catch (IllegalStateException ex) {
            throw new BusinessConflictException("Failed to generate investigator workload report");
        }
    }

    @PostMapping("/investigator-workload/email")
    public ResponseEntity<OpenCasesReportResponseDto> generateAndEmailInvestigatorWorkloadReport() {
        try {
            InvestigatorWorkloadReportResult reportResult = investigatorWorkloadReportService.generateReport("manual-email");
            return ResponseEntity.ok(reportResponseMapper.toResponse(
                    reportResult,
                    true,
                    investigatorWorkloadReportEmailService.sendReportEmail(reportResult)
            ));
        } catch (IllegalStateException ex) {
            throw new BusinessConflictException("Failed to send investigator workload report email");
        }
    }
}
