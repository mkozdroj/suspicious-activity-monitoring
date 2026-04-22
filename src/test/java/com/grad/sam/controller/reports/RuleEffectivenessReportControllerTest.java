package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.service.reports.ruleeffectiveness.RuleEffectivenessReportEmailService;
import com.grad.sam.service.reports.ruleeffectiveness.RuleEffectivenessReportResult;
import com.grad.sam.service.reports.ruleeffectiveness.RuleEffectivenessReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEffectivenessReportControllerTest {

    @Mock
    private RuleEffectivenessReportService ruleEffectivenessReportService;

    @Mock
    private RuleEffectivenessReportEmailService ruleEffectivenessReportEmailService;

    private final ReportResponseMapper reportResponseMapper = new ReportResponseMapper();

    @Test
    void generateRuleEffectivenessReport_returns_generated_response() {
        RuleEffectivenessReportController controller = new RuleEffectivenessReportController(
                ruleEffectivenessReportService,
                ruleEffectivenessReportEmailService,
                reportResponseMapper
        );
        RuleEffectivenessReportResult reportResult = reportResult("manual");
        when(ruleEffectivenessReportService.generateReport("manual")).thenReturn(reportResult);

        ResponseEntity<OpenCasesReportResponseDto> response = controller.generateRuleEffectivenessReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("rules.csv", response.getBody().getReportName());
        assertEquals("manual", response.getBody().getTrigger());
        assertEquals(false, response.getBody().isEmailSent());
        assertEquals(List.of(), response.getBody().getEmailedTo());
    }

    @Test
    void generateAndEmailRuleEffectivenessReport_returns_generated_response() {
        RuleEffectivenessReportController controller = new RuleEffectivenessReportController(
                ruleEffectivenessReportService,
                ruleEffectivenessReportEmailService,
                reportResponseMapper
        );
        RuleEffectivenessReportResult reportResult = reportResult("manual-email");
        when(ruleEffectivenessReportService.generateReport("manual-email")).thenReturn(reportResult);
        when(ruleEffectivenessReportEmailService.sendReportEmail(reportResult)).thenReturn(List.of("a@test.com"));

        ResponseEntity<OpenCasesReportResponseDto> response = controller.generateAndEmailRuleEffectivenessReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("rules.csv", response.getBody().getReportName());
        assertEquals("manual-email", response.getBody().getTrigger());
        assertEquals(true, response.getBody().isEmailSent());
        assertEquals(List.of("a@test.com"), response.getBody().getEmailedTo());
    }

    @Test
    void generateAndEmailRuleEffectivenessReport_throws_business_conflict_when_email_fails() {
        RuleEffectivenessReportController controller = new RuleEffectivenessReportController(
                ruleEffectivenessReportService,
                ruleEffectivenessReportEmailService,
                reportResponseMapper
        );
        when(ruleEffectivenessReportService.generateReport("manual-email")).thenThrow(new IllegalStateException("boom"));

        BusinessConflictException exception = assertThrows(BusinessConflictException.class, controller::generateAndEmailRuleEffectivenessReport);

        assertEquals("Failed to send rule effectiveness report email", exception.getMessage());
    }

    private RuleEffectivenessReportResult reportResult(String trigger) {
        return RuleEffectivenessReportResult.builder()
                .reportName("rules.csv")
                .reportPath(Path.of("/tmp/rules.csv"))
                .recordCount(4)
                .ruleCount(2)
                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .trigger(trigger)
                .rows(List.of())
                .build();
    }
}
