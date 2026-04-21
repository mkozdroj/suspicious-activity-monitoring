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
public class RuleEffectivenessReportController {

    @PostMapping("/rule-effectiveness")
    public ResponseEntity<OpenCasesReportResponseDto> generateRuleEffectivenessReport() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(notImplementedResponse());
    }

    @PostMapping("/rule-effectiveness/email")
    public ResponseEntity<OpenCasesReportResponseDto> generateAndEmailRuleEffectivenessReport() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(notImplementedResponse());
    }

    private OpenCasesReportResponseDto notImplementedResponse() {
        OpenCasesReportResponseDto response = new OpenCasesReportResponseDto();
        response.setReportName("rule-effectiveness");
        response.setTrigger("not-implemented");
        response.setEmailSent(false);
        response.setEmailedTo(java.util.List.of());
        return response;
    }
}
