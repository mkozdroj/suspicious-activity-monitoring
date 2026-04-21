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
public class OpenCasesReportController {

    @PostMapping("/open-cases")
    public ResponseEntity<OpenCasesReportResponseDto> generateOpenCasesReport() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(notImplementedResponse());
    }

    @PostMapping("/open-cases/email")
    public ResponseEntity<OpenCasesReportResponseDto> generateAndEmailOpenCasesReport() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(notImplementedResponse());
    }

    private OpenCasesReportResponseDto notImplementedResponse() {
        OpenCasesReportResponseDto response = new OpenCasesReportResponseDto();
        response.setReportName("open-cases");
        response.setTrigger("not-implemented");
        response.setEmailSent(false);
        response.setEmailedTo(java.util.List.of());
        return response;
    }
}
