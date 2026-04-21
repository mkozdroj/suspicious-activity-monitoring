package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.service.reports.opencases.OpenCasesReportEmailService;
import com.grad.sam.service.reports.opencases.OpenCasesReportResult;
import com.grad.sam.service.reports.opencases.OpenCasesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Validated
@RequiredArgsConstructor
public class OpenCasesReportController {

    private final OpenCasesReportService openCasesReportService;
    private final OpenCasesReportEmailService openCasesReportEmailService;
    private final ReportResponseMapper reportResponseMapper;

    @PostMapping("/open-cases")
    public ResponseEntity<OpenCasesReportResponseDto> generateOpenCasesReport() {
        try {
            OpenCasesReportResult reportResult = openCasesReportService.generateReport("manual");
            return ResponseEntity.ok(reportResponseMapper.toResponse(reportResult));
        } catch (IllegalStateException ex) {
            throw new BusinessConflictException("Failed to generate open cases report");
        }
    }

    @PostMapping("/open-cases/email")
    public ResponseEntity<OpenCasesReportResponseDto> generateAndEmailOpenCasesReport() {
        try {
            OpenCasesReportResult reportResult = openCasesReportService.generateReport("manual-email");
            return ResponseEntity.ok(reportResponseMapper.toResponse(
                    reportResult,
                    true,
                    openCasesReportEmailService.sendReportEmail(reportResult)
            ));
        } catch (IllegalStateException ex) {
            throw new BusinessConflictException("Failed to send open cases report email");
        }
    }
}
