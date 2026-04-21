package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.service.reports.opencases.OpenCasesCategorySummary;
import com.grad.sam.service.reports.opencases.OpenCasesReportEmailService;
import com.grad.sam.service.reports.opencases.OpenCasesReportResult;
import com.grad.sam.service.reports.opencases.OpenCasesReportService;
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
class OpenCasesReportControllerTest {

    @Mock
    private OpenCasesReportService openCasesReportService;

    @Mock
    private OpenCasesReportEmailService openCasesReportEmailService;

    private final ReportResponseMapper reportResponseMapper = new ReportResponseMapper();

    @Test
    void generateOpenCasesReport_returns_generated_response() {
        OpenCasesReportController controller = new OpenCasesReportController(
                openCasesReportService,
                openCasesReportEmailService,
                reportResponseMapper
        );
        OpenCasesReportResult reportResult = reportResult("manual");
        when(openCasesReportService.generateReport("manual")).thenReturn(reportResult);

        ResponseEntity<OpenCasesReportResponseDto> response = controller.generateOpenCasesReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("open.csv", response.getBody().getReportName());
        assertEquals("manual", response.getBody().getTrigger());
        assertEquals(false, response.getBody().isEmailSent());
        assertEquals(List.of(), response.getBody().getEmailedTo());
    }

    @Test
    void generateAndEmailOpenCasesReport_returns_generated_response() {
        OpenCasesReportController controller = new OpenCasesReportController(
                openCasesReportService,
                openCasesReportEmailService,
                reportResponseMapper
        );
        OpenCasesReportResult reportResult = reportResult("manual-email");
        when(openCasesReportService.generateReport("manual-email")).thenReturn(reportResult);
        when(openCasesReportEmailService.sendReportEmail(reportResult)).thenReturn(List.of("a@test.com"));

        ResponseEntity<OpenCasesReportResponseDto> response = controller.generateAndEmailOpenCasesReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("open.csv", response.getBody().getReportName());
        assertEquals("manual-email", response.getBody().getTrigger());
        assertEquals(true, response.getBody().isEmailSent());
        assertEquals(List.of("a@test.com"), response.getBody().getEmailedTo());
    }

    @Test
    void generateAndEmailOpenCasesReport_throws_business_conflict_when_sending_fails() {
        OpenCasesReportController controller = new OpenCasesReportController(
                openCasesReportService,
                openCasesReportEmailService,
                reportResponseMapper
        );
        when(openCasesReportService.generateReport("manual-email")).thenThrow(new IllegalStateException("boom"));

        BusinessConflictException exception = assertThrows(BusinessConflictException.class, controller::generateAndEmailOpenCasesReport);

        assertEquals("Failed to send open cases report email", exception.getMessage());
    }

    private OpenCasesReportResult reportResult(String trigger) {
        return OpenCasesReportResult.builder()
                .reportName("open.csv")
                .reportPath(Path.of("/tmp/open.csv"))
                .recordCount(2)
                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                .trigger(trigger)
                .rows(List.of())
                .categorySummaries(List.of(new OpenCasesCategorySummary("STRUCTURING", 2)))
                .build();
    }
}
