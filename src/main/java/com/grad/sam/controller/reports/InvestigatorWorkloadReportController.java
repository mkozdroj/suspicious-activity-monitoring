package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Validated
public class InvestigatorWorkloadReportController {

    @PostMapping("/investigator-workload")
    public ResponseEntity<OpenCasesReportResponseDto> generateInvestigatorWorkloadReport() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(notImplementedResponse());
    }

    @PostMapping("/investigator-workload/email")
    public ResponseEntity<OpenCasesReportResponseDto> generateAndEmailInvestigatorWorkloadReport() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(notImplementedResponse());
    }

    private OpenCasesReportResponseDto notImplementedResponse() {
        OpenCasesReportResponseDto response = new OpenCasesReportResponseDto();
        response.setReportName("investigator-workload");
        response.setTrigger("not-implemented");
        response.setEmailSent(false);
        response.setEmailedTo(java.util.List.of());
        return response;
    }
}
