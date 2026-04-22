package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.service.reports.workload.InvestigatorWorkloadReportEmailService;
import com.grad.sam.service.reports.workload.InvestigatorWorkloadReportResult;
import com.grad.sam.service.reports.workload.InvestigatorWorkloadReportService;
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
class InvestigatorWorkloadReportControllerTest {

    @Mock
    private InvestigatorWorkloadReportService investigatorWorkloadReportService;

    @Mock
    private InvestigatorWorkloadReportEmailService investigatorWorkloadReportEmailService;

    private final ReportResponseMapper reportResponseMapper = new ReportResponseMapper();

    @Test
    void generateInvestigatorWorkloadReport_returns_generated_response() {
        InvestigatorWorkloadReportController controller = new InvestigatorWorkloadReportController(
                investigatorWorkloadReportService,
                investigatorWorkloadReportEmailService,
                reportResponseMapper
        );
        InvestigatorWorkloadReportResult reportResult = reportResult("manual");
        when(investigatorWorkloadReportService.generateReport("manual")).thenReturn(reportResult);

        ResponseEntity<OpenCasesReportResponseDto> response = controller.generateInvestigatorWorkloadReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("workload.csv", response.getBody().getReportName());
        assertEquals("manual", response.getBody().getTrigger());
        assertEquals(false, response.getBody().isEmailSent());
        assertEquals(List.of(), response.getBody().getEmailedTo());
    }

    @Test
    void generateAndEmailInvestigatorWorkloadReport_returns_generated_response() {
        InvestigatorWorkloadReportController controller = new InvestigatorWorkloadReportController(
                investigatorWorkloadReportService,
                investigatorWorkloadReportEmailService,
                reportResponseMapper
        );
        InvestigatorWorkloadReportResult reportResult = reportResult("manual-email");
        when(investigatorWorkloadReportService.generateReport("manual-email")).thenReturn(reportResult);
        when(investigatorWorkloadReportEmailService.sendReportEmail(reportResult)).thenReturn(List.of("a@test.com"));

        ResponseEntity<OpenCasesReportResponseDto> response = controller.generateAndEmailInvestigatorWorkloadReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("workload.csv", response.getBody().getReportName());
        assertEquals("manual-email", response.getBody().getTrigger());
        assertEquals(true, response.getBody().isEmailSent());
        assertEquals(List.of("a@test.com"), response.getBody().getEmailedTo());
    }

    @Test
    void generateInvestigatorWorkloadReport_throws_business_conflict_when_generation_fails() {
        InvestigatorWorkloadReportController controller = new InvestigatorWorkloadReportController(
                investigatorWorkloadReportService,
                investigatorWorkloadReportEmailService,
                reportResponseMapper
        );
        when(investigatorWorkloadReportService.generateReport("manual")).thenThrow(new IllegalStateException("boom"));

        BusinessConflictException exception = assertThrows(BusinessConflictException.class, controller::generateInvestigatorWorkloadReport);

        assertEquals("Failed to generate investigator workload report", exception.getMessage());
    }

    private InvestigatorWorkloadReportResult reportResult(String trigger) {
        return InvestigatorWorkloadReportResult.builder()
                .reportName("workload.csv")
                .reportPath(Path.of("/tmp/workload.csv"))
                .recordCount(3)
                .analystCount(2)
                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .trigger(trigger)
                .rows(List.of())
                .build();
    }
}
