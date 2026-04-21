package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.service.reports.GeneratedReportResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReportResponseMapperTest {

    private final ReportResponseMapper mapper = new ReportResponseMapper();

    @Test
    void toResponse_maps_report_without_email_metadata() {
        GeneratedReportResult result = new StubReportResult(
                "report.csv",
                Path.of("/tmp/report.csv"),
                7,
                LocalDateTime.of(2026, 4, 21, 12, 0),
                "manual"
        );

        OpenCasesReportResponseDto response = mapper.toResponse(result);

        assertEquals("report.csv", response.getReportName());
        assertEquals("/tmp/report.csv", response.getReportPath());
        assertEquals(7, response.getRecordCount());
        assertEquals(LocalDateTime.of(2026, 4, 21, 12, 0), response.getGeneratedAt());
        assertEquals("manual", response.getTrigger());
        assertFalse(response.isEmailSent());
        assertEquals(List.of(), response.getEmailedTo());
    }

    @Test
    void toResponse_maps_report_with_email_metadata() {
        GeneratedReportResult result = new StubReportResult(
                "report.csv",
                Path.of("/tmp/report.csv"),
                3,
                LocalDateTime.of(2026, 4, 21, 13, 0),
                "scheduler"
        );

        OpenCasesReportResponseDto response = mapper.toResponse(result, true, List.of("a@test.com"));

        assertEquals("report.csv", response.getReportName());
        assertEquals("/tmp/report.csv", response.getReportPath());
        assertEquals(3, response.getRecordCount());
        assertEquals(LocalDateTime.of(2026, 4, 21, 13, 0), response.getGeneratedAt());
        assertEquals("scheduler", response.getTrigger());
        assertEquals(true, response.isEmailSent());
        assertEquals(List.of("a@test.com"), response.getEmailedTo());
    }

    private record StubReportResult(
            String reportName,
            Path reportPath,
            int recordCount,
            LocalDateTime generatedAt,
            String trigger
    ) implements GeneratedReportResult {
        @Override
        public String getReportName() {
            return reportName;
        }

        @Override
        public Path getReportPath() {
            return reportPath;
        }

        @Override
        public int getRecordCount() {
            return recordCount;
        }

        @Override
        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        @Override
        public String getTrigger() {
            return trigger;
        }
    }
}
